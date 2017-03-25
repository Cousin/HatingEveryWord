package xyz.betanyan.hatebot;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class HateBot {

    public static void main(String[] args) throws Exception {

        boolean exists = true;

        File indexFile = new File("index.txt");
        if (!indexFile.exists()) {
            exists = false;
            indexFile.createNewFile();
        }

        if (!exists) {
            PrintWriter indexWriter = new PrintWriter("index.txt");
            indexWriter.write("0");
            indexWriter.flush();
            indexWriter.close();
        }

        int index = Integer.parseInt(Files.readAllLines(indexFile.toPath()).get(0));

        List<String> words = Files.readAllLines(new File("words.txt").toPath());

        List<String> toTweet = new CopyOnWriteArrayList<>();
        toTweet.addAll(words.subList(index, words.size() - 1));

        ConfigurationBuilder cb = new ConfigurationBuilder();

        cb.setDebugEnabled(false);
        cb.setOAuthConsumerKey(APIDetails.getOAuthConsumerKey());
        cb.setOAuthConsumerSecret(APIDetails.getOAuthConsumerSecret());
        cb.setOAuthAccessToken(APIDetails.getOAuthAccessToken());
        cb.setOAuthAccessTokenSecret(APIDetails.getOAuthAccessTokenSecret());

        try {
            new HateBot(new TwitterFactory(cb.build()).getInstance(), indexFile, toTweet, index);
        } catch (IOException e) {
            System.err.println("There was an error starting the bot:");
            System.err.println(e.getMessage());
        }

        System.out.println("Bot started.");

    }

    private Twitter twitter;
    private List<String> toTweet;
    private int index;
    private File indexFile;

    public HateBot(Twitter twitter, File indexFile, List<String> toTweet, int index) throws IOException {

        this.twitter = twitter;
        this.toTweet = toTweet;
        this.indexFile = indexFile;
        this.index = index;

        PrintWriter writer = new PrintWriter(new FileOutputStream(indexFile, false));

        AtomicInteger currentIndex = new AtomicInteger(index);

        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
        service.scheduleAtFixedRate(() -> {

            String word = toTweet.get(currentIndex.get()).toLowerCase();
            if (word.endsWith("s")) {
                try {
                    twitter.updateStatus("i hate " + word);
                } catch (TwitterException e) {}
            } else {
                try {
                    twitter.updateStatus("i hate the " + word);
                } catch (TwitterException e) {}
            }

            System.out.println("Tweeted " + word);

            writer.write(String.valueOf(currentIndex.incrementAndGet()));
            writer.flush();

        }, 25, 90, TimeUnit.SECONDS);


    }

}
