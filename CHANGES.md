# jsoup Changelog

## 1.17.2 (Pending)

### Improvements
* Added `Element.attribute(String)` and `Attributes.attribute(String)` to more simply obtain an `Attribute` object. 
  [2069](https://github.com/jhy/jsoup/issues/2069)
* If source tracking is on, and an Attribute's key is changed (via `Attribute.setKey(String)`), the source range is 
  now still tracked in `Attribute.sourceRange()`. [2070](https://github.com/jhy/jsoup/issues/2070)

### Bug Fixes

* When tracking the source position of attributes, if source attribute name was mix-cased but the parser was
  lower-case normalizing attribute names, the source position for that attribute was not tracked
  correctly. [2067](https://github.com/jhy/jsoup/issues/2067)
* When tracking the source position of a body fragment parse, a null pointer exception was
  thrown. [2068](https://github.com/jhy/jsoup/issues/2068)

---
Older changes for versions 0.1.1 (2010-Jan-31) through 1.17.1 (2023-Nov-27) may be found in
[change-archive.txt](./change-archive.txt).
