package org.jsoup.validators;

import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.jsoup.constraints.SafeHtml;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;

/**
 * Validate that the string does not contain malicious code.
 *
 * It uses <a href="https://www.jsoup.org">JSoup</a> as the underlying parser/sanitizer library.
 *
 * @author George Gastaldi
 * @author Hardy Ferentschik
 * @author Marko Bekhta
 * @author Craig Andrews
 */
public class SafeHtmlValidator implements ConstraintValidator<SafeHtml, CharSequence> {
	private Whitelist whitelist;

	private String baseURI;

	@Override
	public void initialize(SafeHtml safeHtmlAnnotation) {
		switch ( safeHtmlAnnotation.whitelistType() ) {
			case BASIC:
				whitelist = Whitelist.basic();
				break;
			case BASIC_WITH_IMAGES:
				whitelist = Whitelist.basicWithImages();
				break;
			case NONE:
				whitelist = Whitelist.none();
				break;
			case RELAXED:
				whitelist = Whitelist.relaxed();
				break;
			case SIMPLE_TEXT:
				whitelist = Whitelist.simpleText();
				break;
		}
		baseURI = safeHtmlAnnotation.baseURI();
		whitelist.addTags( safeHtmlAnnotation.additionalTags() );

		for ( SafeHtml.Tag tag : safeHtmlAnnotation.additionalTagsWithAttributes() ) {
			whitelist.addTags( tag.name() );
			if ( tag.attributes().length > 0 ) {
				whitelist.addAttributes( tag.name(), tag.attributes() );
			}

			for ( SafeHtml.Attribute attribute : tag.attributesWithProtocols() ) {
				whitelist.addAttributes( tag.name(), attribute.name() );

				if ( attribute.protocols().length > 0 ) {
					whitelist.addProtocols( tag.name(), attribute.name(), attribute.protocols() );
				}
			}
		}
	}

	@Override
	public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
		if ( value == null ) {
			return true;
		}

		return new Cleaner( whitelist ).isValid( getFragmentAsDocument( value ) );
	}

	/**
	 * Returns a document whose {@code <body>} element contains the given HTML fragment.
	 */
	private Document getFragmentAsDocument(CharSequence value) {
		// using the XML parser ensures that all elements in the input are retained, also if they actually are not allowed at the given
		// location; E.g. a <td> element isn't allowed directly within the <body> element, so it would be used by the default HTML parser.
		// we need to retain it though to apply the given white list properly; See HV-873
		Document fragment = Jsoup.parse( value.toString(), baseURI, Parser.xmlParser() );
		Document document = Document.createShell( baseURI );

		// add the fragment's nodes to the body of resulting document
		List<Node> childNodes = fragment.childNodes();
		for ( Node node : childNodes ) {
			document.body().appendChild( node.clone() );
		}

		return document;
	}
}
