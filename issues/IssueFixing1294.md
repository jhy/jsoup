# Issues
## Issue 1294
### Problem Discreption
Given a ruby html segment, Jsoup cannot parse it correctly.

![](C:\Users\xingx\AppData\Roaming\Typora\typora-user-images\image-20200420171640816.png)

![](C:\Users\xingx\AppData\Roaming\Typora\typora-user-images\image-20200420171730747.png)

The results in the second picture shows that Jsoup terminate `<rtc>`tag ahead of schedule.

### Problem Reasons

The problem is caused by the implementation of how Jsoup handle tags. It create an enumeration that enumerate all possible tags it would meet. For those unknown tags, Jsoup will close it directly. More detailed code could be seen in the org.jsoup.parser.HtmlTreeBuilderParser, line 581.

![](https://imgur.com/CUX6qXL.png)

It will firstly check whether `<ruby>` tag had been pushed into stack since tag `<rp>` and `<rt>` must appeared as a child node of ruby node. Besides, it will generate end tag and pop out all node until its current node is ruby node. This logic implies that only `<rb>` and `<rt>` must be direct child of  jsoup.

### Problem Solution

There will be not more than `rb` and `rt` tag in `ruby` tag. Besides, someone could add `div` or `span` to which is meaningless but to divide up some area which maybe used to apply CSS.

![](https://imgur.com/hJ0zwPu.png)

So the best way to deal with it is to deal it as a normal tag and just use default way to handle it.

![](https://imgur.com/X9CXqko.png)

Now Jsoup hanld it correctly.

![](https://imgur.com/aNawR6S.png)