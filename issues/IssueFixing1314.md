## Issue 1294

### User Story

Jsoup, as a powerful third-part library to download and parse html segment from remote servers, had been widely used in various Java-write crawlers. When crawlers work, it parse various html tags by multi levels. Each level represent a tag, and a corresponding query syntax. If Jsoup cannot deliver right level logic, crawler will not work normally as what developers expected.

### Problem Description

Given a ruby html segment, Jsoup cannot parse it correctly.

![](https://imgur.com/irw8ZVa.png)

![](https://imgur.com/7nM7ckt.png)

The results in the second picture shows that Jsoup terminate `<rtc>`tag ahead of schedule. What we respect here is

```html
<html>
	<head></head>
	<body>
		<ruby>
			<rtc>
				<rt>2</rt>
			</rtc>
		</ruby>
	</body>
</html>
```

### Problem Reasons

This problem is caused by the implementation that how Jsoup handle tags. It create an enumeration that lists all possible tags it would meet.(more details could be seen in `src.main.java.org.jsoup.parser.HtmlTreeBuildState.java`) For those unknown tags, Jsoup will close it directly. However, it add a special judgment for ruby tags. More detailed code could be seen in the following code segment which is cut from original project.

![](https://imgur.com/CUX6qXL.png)

It will firstly check whether `<ruby>` tag had been pushed into stack since tag `<rp>` and `<rt>` must appeared as a child node of ruby node. Besides, it will generate end tag and pop out all node until its current node is ruby node. This logic implies that only `<rb>` and `<rt>` must be direct child of  Jsoup.

### Problem Solution

There will be not more than `rb` and `rt` tag in `ruby` tag. Besides, someone could add `div` or `span` to which is meaningless but to divide up some area which maybe used to apply cascading style sheet. For instance, if we add an div tag in it, it is definitely wrong to close it in advance.

![](https://imgur.com/hJ0zwPu.png)

So the best way to deal with it is to deal it as a normal tag and just use default way to handle it.

![](https://imgur.com/X9CXqko.png)

Now Jsoup can process it correctly.

![](https://imgur.com/aNawR6S.png)