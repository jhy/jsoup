# Issues
## Issue 1344
### Problem Discreption
The problem happens when the user tries to parse the title of a xml file with org.Jsoup.Document.title() method. The problem is that the method will not strip the children nodes in the title tag, which behaves different with browers. 

Here is the screen shot of the problem:
![](https://i.imgur.com/QyJSCJT.png)

![](https://i.imgur.com/UNNCOo2.png)
Problem result: 

```
normal &lt;3 alert() bold !
```

### Problem Reasons
The problem is caused by the implementation of how Jsoup handle xml file. It create a stack and push the nodes with children nodes in them into the stack and then keep those nodes. You can check the implementation in parser/XMLTreeBuild.java .
So the tree structure is like this:
```HTML

level-0:<html xmlns="http://www.w3.org/1999/xhtml"><head><title>normal &lt;3 <script>alert()</script><b>bold</b>!</title></head></html>

level-1:<head><title>normal &lt;3 <script>alert()</script><b>bold</b>!</title></head>

level-2: <title>normal &lt;3 <script>alert()</script><b>bold</b>!</title>

level-3:normal &lt;3, <script>alert()</script>, b>bold</b>, !

```

But when handling the title() method. The Document class calling the method getElementsByTag() in Elemet class, which use the tagName to match the tag starting with root node. Then the getElementsByTag() will return the elements with tagName "title". However, due to the tree structure fact mentioned above, the return Elements is actually a list with children nodes. 
```
<title>normal &lt;3 <script>alert()</script><b>bold</b>!</title>
```

Then there comes with the final reason, the text() method in the Element class only chech the tagName, which cause it also add the children nodes. 

### Problem Solution
The solution is simple, we just remove all the children nodes in the title tag, but it is hard to do so. Therefore, we just add text to the final result. 
Here is the solution in the Document class:
![](https://i.imgur.com/wNjaXEJ.png)
