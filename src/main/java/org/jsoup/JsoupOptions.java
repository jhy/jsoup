package org.jsoup;

/**
 The Jsoup options.

 @author Daniel Kurka */
public class JsoupOptions {

  public static final JsoupOptions DEFAULT_OPTIONS =
      new Builder().normalizeAttributes(true).build();

  private boolean normalizeAttributes;

  /**
   Should Jsoup normalize attributes. This will turn all attribute keys into
   lowercase.
   */
  public boolean shouldNormalizeAttributes() {
    return normalizeAttributes;
  }

  /**
   A builder to create a JsoupOptions object.
  */
  public static class Builder {
    private JsoupOptions options = new JsoupOptions();

    /**
     Should we normalize attributes:

     This would turn an uppercase attribute key into all lowercase.
     */
    public Builder normalizeAttributes(boolean normalize) {
      options.normalizeAttributes = normalize;
      return this;
    }

    public JsoupOptions build() {
      return options;
    }
  }
}
