package org.jsoup.experimental;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

public class Test {
//	@ToString
	public static class Person {
		// No selector specified = The element passed when calling
		// getObjectFromElement() is where the attribute will be pulled from.
		@JAttribute("id") 
		private long id;
		
		@JAttribute(value = "full", selector = "name")
		private String fullName;
		
		@JSelector("name")
		private Names names;
		
		@JSelector // Remember: @JSelector = @JSelector(javaFieldName)
		private short age;
		
		@JSelector("isMarried")
		private boolean married;
		
		@JSelector("gender")
		// Though we specified a converter class, we can just leave the argument
		// blank to use the enum's valueOf() method.
		@JEnumConvert(GenderConverter.class)
		private Gender gender;
		
		@JSelector("jobs > item")
		private List<Job> jobs;
		
		// This field will be ignored (null): no @JSelector or @JAttribute specified.
		private String ignoreMe;
		
		// This field will also be ignored (null): our 
		// example XML does not contain an element named "ignoreMeAlso"
		@JSelector("ignoreMeAlso")
		private String ignoreMeAlso;
	}
	
//	@ToString
	public static class Names {
		@JSelector("first")
		private String firstName;
		@JSelector
		private String last;
	}
	
//	@ToString
	public static class Job {
		@JSelector
		private String employer;
		@JSelector
		private Double hourly;
		@JSelector
		private Long salary;
	}
	
//	@ToString
	public static enum Gender {
		MALE, FEMALE, OTHER
	}
	
	public static class GenderConverter implements JEnumConverter<Gender> {
		@Override
		public Gender getEnum(String raw) {
			System.out.println("Gender converter called!");
			return Gender.valueOf(raw.toUpperCase());
		}
	}
	
	public static void main(String[] args) throws IOException {
		final JsoupConverter jc = new JsoupConverter();
		
		final Document doc = Jsoup.parse(new File("example.xml"), "UTF-8", "", Parser.xmlParser());
		
		final Person person = jc.getObjectFromElement(doc.selectFirst("person"), Person.class);
		
		System.out.println(doc);
		System.out.println(person);
	}
}
