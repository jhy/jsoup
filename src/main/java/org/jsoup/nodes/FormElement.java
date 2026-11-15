package org.jsoup.nodes;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.helper.HttpConnection;
import org.jsoup.helper.Validate;
import org.jsoup.internal.SharedConstants;
import org.jsoup.internal.StringUtil;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;
import org.jsoup.select.Selector;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * An HTML Form Element provides ready access to the form fields/controls that are associated with it. It also allows a
 * form to easily be submitted.
 */
public class FormElement extends Element {
    private final ArrayList<Element> linkedEls = new ArrayList<>();
    // contains form submittable elements that were linked during the parse (and due to parse rules, may no longer be a child of this form)
    private static final Evaluator submittable = Selector.evaluatorOf(StringUtil.join(SharedConstants.FormSubmitTags, ", "));

    /**
     * Create a new, standalone form element.
     *
     * @param tag        tag of this element
     * @param baseUri    the base URI
     * @param attributes initial attributes
     */
    public FormElement(Tag tag, @Nullable String baseUri, @Nullable Attributes attributes) {
        super(tag, baseUri, attributes);
    }

    /**
     * Get the list of form control elements associated with this form.
     * @return form controls associated with this element.
     */
    public Elements elements() {
        // As elements may have been added or removed from the DOM after parse, prepare a new list that unions them:
        Elements els = select(submittable); // current form children
        for (Element linkedEl : linkedEls) {
            if (linkedEl.ownerDocument() != null && !els.contains(linkedEl)) {
                els.add(linkedEl); // adds previously linked elements, that weren't previously removed from the DOM
            }
        }

        return els;
    }

    /**
     * Add a form control element to this form.
     * @param element form control to add
     * @return this form element, for chaining
     */
    public FormElement addElement(Element element) {
        linkedEls.add(element);
        return this;
    }

    @Override
    protected void removeChild(Node out) {
        super.removeChild(out);
        linkedEls.remove(out);
    }

    /**
     Removes all child nodes from this form in the DOM, leaving the form itself in place.
     Controls associated with this form but located elsewhere in the document remain unchanged.

     @return this form
     */
    @Override
    public Element empty() {
        // clear descendant associations before their parent links are removed
        linkedEls.removeIf(this::isDescendant);
        return super.empty();
    }

    private boolean isDescendant(Element element) {
        for (Element parent = element.parent(); parent != null; parent = parent.parent()) {
            if (parent == this) return true;
        }
        return false;
    }

    /**
     Prepare to submit this form. A Connection object is created with the request set up from the form values. This
     Connection will inherit the settings and the cookies (etc) of the connection/session used to request this Document
     (if any), as available in {@link Document#connection()}
     <p>You can then set up other options (like user-agent, timeout, cookies), then execute it.</p>

     @return a connection prepared from the values of this form, in the same session as the one used to request it
     @throws IllegalArgumentException if the form's absolute action URL cannot be determined. Make sure you pass the
     document's base URI when parsing.
     */
    public Connection submit() {
        String action = hasAttr("action") ? absUrl("action") : baseUri();
        Validate.notEmpty(action, "Could not determine a form action URL for submit. Ensure you set a base URI when parsing.");
        Connection.Method method = attr("method").equalsIgnoreCase("POST") ?
                Connection.Method.POST : Connection.Method.GET;

        Document owner = ownerDocument();
        Connection connection = owner != null? owner.connection().newRequest() : Jsoup.newSession();
        return connection.url(action)
                .data(formData())
                .method(method);
    }

    /**
     * Get the data that this form submits. The returned list is a copy of the data, and changes to the contents of the
     * list will not be reflected in the DOM.
     * @return a list of key vals
     */
    public List<Connection.KeyVal> formData() {
        ArrayList<Connection.KeyVal> data = new ArrayList<>();

        // iterate the form control elements and accumulate their values
        Elements formEls = elements();
        for (Element el: formEls) {
            if (!el.tag().isFormSubmittable()) continue; // contents are form listable, superset of submitable
            if (el.hasAttr("disabled")) continue; // skip disabled form inputs
            String name = el.attr("name");
            if (name.length() == 0) continue;
            String type = el.attr("type");

            if (type.equalsIgnoreCase("button") || type.equalsIgnoreCase("image")) continue; // browsers don't submit these

            if (el.nameIs("select")) {
                Elements options = el.select("option[selected]");
                boolean set = false;
                for (Element option: options) {
                    data.add(HttpConnection.KeyVal.create(name, optionValue(option)));
                    set = true;
                }
                if (!set) {
                    // no option is selected; a single-select with display size 1 defaults to its first enabled
                    // option, while multiple selects and listboxes submit nothing (HTML selectedness algorithm)
                    Element option = defaultSelectedOption(el);
                    if (option != null)
                        data.add(HttpConnection.KeyVal.create(name, optionValue(option)));
                }
            } else if ("checkbox".equalsIgnoreCase(type) || "radio".equalsIgnoreCase(type)) {
                // only add checkbox or radio if they have the checked attribute
                if (el.hasAttr("checked")) {
                    final String val = el.val().length() >  0 ? el.val() : "on";
                    data.add(HttpConnection.KeyVal.create(name, val));
                }
            } else {
                data.add(HttpConnection.KeyVal.create(name, el.val()));
            }
        }
        return data;
    }

    // The value of an option is its value attribute when it has one, and its text otherwise.
    private static String optionValue(Element option) {
        return option.hasAttr("value") ? option.val() : option.text();
    }

    // The option a select defaults to when nothing is selected: the first option that is not disabled, but only
    // for a single-select with display size 1. Multiple selects and listboxes have no default selection.
    private static @Nullable Element defaultSelectedOption(Element select) {
        if (select.hasAttr("multiple")) return null;
        if (displaySize(select) != 1) return null;
        for (Element option : select.select("option")) {
            if (!isDisabled(option)) return option;
        }
        return null;
    }

    // Display size from the size attribute, defaulting to 1 (or 4 for a multiple select, which never reaches here).
    private static int displaySize(Element select) {
        String size = select.attr("size");
        if (!size.isEmpty() && StringUtil.isNumeric(size)) {
            try {
                int parsed = Integer.parseInt(size);
                if (parsed > 0) return parsed;
            } catch (NumberFormatException ignored) {
                // fall through to the default
            }
        }
        return 1;
    }

    // An option is disabled when it carries the disabled attribute, or sits in a disabled optgroup.
    private static boolean isDisabled(Element option) {
        if (option.hasAttr("disabled")) return true;
        Element parent = option.parent();
        return parent != null && parent.nameIs("optgroup") && parent.hasAttr("disabled");
    }

    @Override
    public FormElement clone() {
        return (FormElement) super.clone();
    }
}
