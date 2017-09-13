package com.cookware.home.server.MediaManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Created by Kody on 13/09/2017.
 */
public class DownloadManager {
    // TODO: Convert all System.out.print statements to log.info

    private static final Logger log = Logger.getLogger(DownloadManager.class);
    private final WebTool webTool = new WebTool();
    private final String moviePath = "C:\\Users\\maste\\IdeaProjects\\CookwareHomeServer\\Movies";
    private final String tvPath = "C:\\Users\\maste\\IdeaProjects\\CookwareHomeServer\\TV";
    private String fileType;


    public boolean downloadMedia(MediaInfo mediaInfo){
        String embeddedMediaUrl = bridgeToVideoMe(mediaInfo.URL, mediaInfo.QUALITY);
        log.info(embeddedMediaUrl);
        // TODO: Change newDownload to take in MediaInfo as a parameter to properly construct filenames
        newDownload(embeddedMediaUrl, mediaInfo.NAME + fileType, mediaInfo.TYPE);


        return true;
    }

    private String bridgeToVideoMe(String url, int quality){
        String html = webTool.getWebPageHtml(url);
        String videoMeUrl = webTool.extractBaseURl(url) + findVideoMeLinkInHtml(html);
        String redirectedUrl = webTool.getRedirectedUrl(videoMeUrl);
        List<DownloadLink> mediaDownloadLinks = extractAllMediaUrls(redirectedUrl);
        String embeddedMediaUrl = selectBestLinkByQuality(mediaDownloadLinks, quality);


        return embeddedMediaUrl;
    }


    public String findVideoMeLinkInHtml(String html){
        Document document = Jsoup.parse(html);
        Elements matchedLinks = document.getElementsByTag("table");
        if(matchedLinks.isEmpty()){
            System.out.println("No entries found, please try again!");

            return null;
        }

        int i = 1;
        String site;
        String url = "";
        for (Element matchedLink : matchedLinks) {
            if(matchedLink.hasAttr("class")) {
                site = "";
                try{
                    site = matchedLink.getElementsByClass("version_host").tagName("script").html().split("'")[1];
                }catch(Exception e){
                }
//                System.out.println(String.format("Op %d: %s", i, site));
                if(site.equals("thevideo.me")){
                    url = matchedLink.getElementsByAttribute("href").attr("href");
                    break;
                }
                i++;
            }
        }
        return url;
    }


    public List<DownloadLink> extractAllMediaUrls(String url){
        // TODO: Clean up this function
        Scanner scan;
        String logicalLine;
        String firstPage = webTool.getWebPageHtml(url);
        Document document = Jsoup.parse(firstPage);
        String hash = document.getElementsByAttributeValue("name", "hash").get(0).attr("value");

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("_vhash", "i1102394cE"));
        params.add(new BasicNameValuePair("gfk", "i22abd2449"));
        params.add(new BasicNameValuePair("hash", hash));
        params.add(new BasicNameValuePair("inhu", "foff"));

        String secondPage = webTool.getWebPageHtml(url, WebTool.HttpRequestType.POST, params);

        int startOfUrlCodeInWebPage = secondPage.indexOf("lets_play_a_game='");

        scan = new Scanner(secondPage.substring(startOfUrlCodeInWebPage+"lets_play_a_game='".length()));
        scan.useDelimiter(Pattern.compile("'"));
        logicalLine = scan.next();

        String thirdPage = webTool.getWebPageHtml("https://thevideo.me/vsign/player/"+logicalLine);

        String[] encodedAttributes = thirdPage.split("\\|");

        String encodedHash = "";
        for (String temp:encodedAttributes){
            if(temp.length()==282){
                encodedHash = temp;
                break;
            }
        }

        int startOfLinksInWebPage = secondPage.indexOf("sources: [");
        scan = new Scanner(secondPage.substring(startOfLinksInWebPage+11));
        scan.useDelimiter(Pattern.compile("}]"));
        logicalLine = scan.next();
        String[] rawMediaSources = logicalLine.split("\\},\\{");

        List<DownloadLink> mediaLinks = new ArrayList<DownloadLink>();
        for (String source:rawMediaSources){
            String[] rawSeperatedValues = source.split("\"");
            try{
                String downloadUrl = rawSeperatedValues[3];
                // TODO: Find another method other than a global to transfer the fileType
                fileType = downloadUrl.substring(downloadUrl.length()-4);

                int quality = Integer.parseInt(rawSeperatedValues[7].replaceAll("[^0-9]", ""));
                mediaLinks.add(new DownloadLink(downloadUrl + "?direct=false&ua=1&vt=" + encodedHash, quality));
            } catch (Exception e){
                log.error("Seperation of download links failed");
            }
        }

        return mediaLinks;
    }


    private String selectBestLinkByQuality(List<DownloadLink> mediaLinks, int quality){
        // TODO: Update this method to accept all qualities

        int bestQuality = 0;
        String result = "";
        for (DownloadLink mediaLink:mediaLinks){
            if(bestQuality < mediaLink.quality){
                result = mediaLink.url;
                bestQuality = mediaLink.quality;
            }
        }
        return result;
    }


    public void newDownload(String downloadUrl, String downloadFilename, MediaType type){
        String downloadFilepath = "";
        if(type == MediaType.MOVIE) {
            downloadFilepath = this.moviePath;
        }
        else {
            downloadFilepath = this.tvPath;
        }
        File output = new File(downloadFilepath, downloadFilename);
        try {
            downloadMediaToFile(downloadUrl, output);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }


    private void downloadMediaToFile(String downloadUrl, File outputfile) throws Throwable {
        HttpGet httpget2 = new HttpGet(downloadUrl);
        long startTime = System.currentTimeMillis();

        System.out.println("Executing " + httpget2.getURI());
        HttpClient httpclient2 = new DefaultHttpClient();
        HttpResponse response2 = httpclient2.execute(httpget2);
        HttpEntity entity2 = response2.getEntity();
        if (entity2 != null && response2.getStatusLine().getStatusCode() == 200) {
            long length = entity2.getContentLength();
            InputStream instream2 = entity2.getContent();
            System.out.println("Writing " + length + " bytes to " + outputfile);
            if (outputfile.exists()) {
                outputfile.delete();
            }
            FileOutputStream outstream = new FileOutputStream(outputfile);
            int i = 1;
            try {
                byte[] buffer = new byte[2048];
                int count = -1;
                while ((count = instream2.read(buffer)) != -1) {
                    printProgress(startTime, (int) length/2048+1, i);
                    i++;
                    outstream.write(buffer, 0, count);
                }
                outstream.flush();
            } finally {
                outstream.close();
            }
        }
    }


    private void printProgress(long startTime, long total, long current) {
        long eta = current == 0 ? 0 :
                (total - current) * (System.currentTimeMillis() - startTime) / (current);


        String etaHms = current == 0 ? "N/A" :
                String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(eta),
                        TimeUnit.MILLISECONDS.toMinutes(eta) % TimeUnit.HOURS.toMinutes(1),
                        TimeUnit.MILLISECONDS.toSeconds(eta) % TimeUnit.MINUTES.toSeconds(1));

        StringBuilder string = new StringBuilder(140);
        int percent = (int) (current * 100 / total);
        string
                .append('\r')
                .append(String.join("", Collections.nCopies(percent == 0 ? 2 : 2 - (int) (Math.log10(percent)), " ")))
                .append(String.format(" %d%% [", percent))
                .append(String.join("", Collections.nCopies(percent, "=")))
                .append('>')
                .append(String.join("", Collections.nCopies(100 - percent, " ")))
                .append(']')
                .append(String.join("", Collections.nCopies((int) (Math.log10(total)) - (int) (Math.log10(current)), " ")))
                .append(String.format(" %.2f/%.2fMB ", ((double) current)/512, ((double) total)/512))
                .append(String.format("(%.2fMB/s), ", ((double) current)/512/TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime)))
                .append(String.format("ETA: %s", etaHms));

        System.out.print(string);
    }


    public class DownloadLink{
        public String url;
        public int quality;

        private DownloadLink(String mUrl, int mQuality){
            this.url = mUrl;
            this.quality = mQuality;
        }

        public String toString(){
            return String.format("(%dp) %s",this.quality, this.url);
        }
    }
}
