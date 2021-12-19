package org.jsoup.examples;

public class RankInfo{
	public String rank, distro, hits, change;

	public RankInfo(){}

	public RankInfo(String rank, String distro, String hits, String change){
		this.rank = rank;
		this.distro = distro;
		this.hits = hits;
		this.change = change;
	}

	public String getRank(){
		return rank;
	}

	public void setRank(String rank){
		this.rank = rank;
	}

	public String getDistro(){
		return distro;
	}

	public void setDistro(String distro){
		this.distro = distro;
	}

	public String getHits(){
		return hits;
	}

	public void setHits(String hits){
		this.hits = hits;
	}

	public String getChange(){
		return change;
	}

	public void setChange(String change){
		this.change = change;
	}
}