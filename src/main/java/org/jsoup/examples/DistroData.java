package org.jsoup.examples;

import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 * Along side the RankInfo class, this gathers the 7 day trend for Linux
 * Distribution on Distro Watch (https://distrowatch.com)
 * 
 * @author Damien Hunter <damien.hunter9@gmail.com>
 *
 */
public class DistroData{
	public DistroData(){}

	public static List<RankInfo> sevenDay() throws Exception{
		Document doc = Jsoup.connect("https://distrowatch.com/index.php?dataspan=4").get();
		Elements rank = doc.getElementsByClass("phr1"), distro = doc.getElementsByClass("phr2"), hits = doc.getElementsByClass("phr3");
		String rankString = "", distroString = "", hitsString = "", change = "";
		List<RankInfo> objects = new ArrayList<RankInfo>();
		RankInfo distroObj;

		for(int x = 0; x < 5; x++){
			rankString = rank.get(x).toString().replace("<th class=\"phr1\">", "").replace("</th>", "");
			distroString = distro.get(x).toString().replace("</a></td>", "");
			distroString = distroString.replace(distroString.substring(0, distroString.lastIndexOf(">") + 1), "");

			hitsString = hits.get(x).toString();
			hitsString = hitsString.replace(hitsString.substring(0, hitsString.indexOf(":") + 2), "");
			hitsString = hitsString.replace(hitsString.substring(hitsString.indexOf("<"), hitsString.length()), "").replace("\">", " ");

			String[] changes = hitsString.split(" ");
			int a = Integer.parseInt(changes[0]), b = Integer.parseInt(changes[1]), c = 0;

			if(a > b){
				c = a - b;
				change = "Down";
			}
			if(b > a){
				c = b - a;
				change = "Up";
			}
			else{
				c = a - b;
				change = "No Change";
			}

			hitsString = "Hits : " + b;
			change = change + " With " + c + " Hits Since Yesterday\n";

			distroObj = new RankInfo(rankString, distroString, hitsString, change);
			objects.add(distroObj);
		}
		return objects;
	}

	public static void main(String[] args) throws Exception{
		DistroData.sevenDay();
	}
}