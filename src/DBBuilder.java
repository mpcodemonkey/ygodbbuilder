import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;
import org.jsoup.Connection.Response;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by Jon on 1/12/2015.
 */

public class DBBuilder {
    public HashMap<String, String> cardLinkTable = null;
    public HashMap<String, YgoSet> setRefTable = null;
    public ArrayList<YgoCard> cardsToInsert = null;

    public void start(){
        String allCardsUrl = null;
        cardsToInsert = new ArrayList<>();
        cardLinkTable = new HashMap<>();
        setRefTable = new HashMap<>();
        ArrayList<String> cardNames = null;
        String jsonString = null;
        JSONObject cardInfoObject = null;
        JSONArray cardSiteInfo = null;
        //get all current card names from the tcg database
        //todo: logic to make limit value not hardcoded
        allCardsUrl = "http://yugioh.wikia.com/api/v1/Articles/List?category=TCG_cards&limit=7000&namespaces=0";

        try{
            jsonString = Jsoup.connect(allCardsUrl).timeout(25000).ignoreContentType(true).execute().body();
        }catch(java.io.IOException e){
            e.printStackTrace();
            System.exit(1);
        }

        cardInfoObject = new JSONObject(jsonString);

        cardSiteInfo = cardInfoObject.getJSONArray("items");

        cardNames = new ArrayList<String>();

        for(int i = 0; i < cardSiteInfo.length(); i++){
            String cardName = cardSiteInfo.getJSONObject(i).getString("title");
            if (!cardLinkTable.containsKey(cardName)) {
                cardNames.add(cardName);
                String tmp = cardSiteInfo.getJSONObject(i).getString("url");
                cardLinkTable.put(cardName, tmp);
            }
        }

        parseCardInformation(cardNames);

        //todo: change to database inserts
        try{
            Class.forName("com.mysql.jdbc.Driver");
        }
        catch (ClassNotFoundException c){
            c.printStackTrace();
        }
        Connection conn = null;
        try{

            conn = DriverManager.getConnection("jdbc:mysql://104.131.189.31:3306/ygo_rebuild?user=generic&password=generic11PASSWORD");

            for(YgoSet y : setRefTable.values()){
                String sql = "INSERT INTO stt_yugioh_set (yst_uid, yst_name, yst_release_date) VALUES (?,?,?) ";
                PreparedStatement stmt = null;
                try{
                    stmt = conn.prepareStatement(sql);
                }
                catch(SQLException s){
                    s.printStackTrace();
                }

                System.out.println("Inserting " + y.getYstName());

                stmt.setString(1, y.getYstIdentifier());
                stmt.setString(2, y.getYstName());
                stmt.setString(3, y.getYstReleaseDate());

                stmt.execute();
            }

            for(YgoCard y : cardsToInsert){
                String sql = "INSERT INTO stt_yugioh_card (ycr_name, ycr_super_type, ycr_set_id, ycr_rarity, ycr_type, " +
                             "ycr_attribute, ycr_card_effect_type, ycr_level, ycr_rank, ycr_atk, ycr_def, ycr_flavor_text, " +
                             "ycr_pendulum_scale, ycr_pendulum_flavor, ycr_image_name, ycr_icon, ycr_monster_type) " +
                             "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";
                PreparedStatement stmt = null;
                try{
                    stmt = conn.prepareStatement(sql);
                }
                catch(SQLException s){
                    s.printStackTrace();
                }

                System.out.println("Inserting " + y.getYcrName());

                stmt.setString(1, y.getYcrName());
                stmt.setString(2, y.getYcrSuperType());
                stmt.setString(3, y.getYcrSetID());
                stmt.setString(4, y.getYcrRarity());
                stmt.setString(5, y.getYcrType());
                stmt.setString(6, y.getYcrAttribute());
                stmt.setString(7, y.getYcrCardEffectType());
                stmt.setInt(8, y.getYcrLevel());
                stmt.setInt(9, y.getYcrRank());
                stmt.setInt(10, y.getYcrAtk());
                stmt.setInt(11, y.getYcrDef());
                stmt.setString(12, y.getYcrFlavorText());
                stmt.setInt(13, y.getYcrPendulumScale());
                stmt.setString(14, y.getYcrPendulumFlavor());
                stmt.setString(15, y.getYcrImageName());
                stmt.setString(16, y.getYcrIcon());
                stmt.setString(17, y.getYcrMonsterType());


                stmt.execute();
            }

            conn.close();
        }

        catch(SQLException s){
            s.printStackTrace();
        }


    }

    private void parseCardInformation(ArrayList<String> cardNames){
        String cardUrl = null;
        for(String cardName : cardNames) {

            System.out.println("working on " + cardName);

            YgoCard card = new YgoCard();
            card.setYcrName(cardName);
            cardUrl = "http://yugioh.wikia.com" + cardLinkTable.get(cardName);
            //cardUrl = "http://yugioh.wikia.com/wiki/Witch_of_the_Black_Rose";
            String cardHtml = null;
            try {
                cardHtml = Jsoup.connect(cardUrl).timeout(400000).ignoreContentType(true).execute().body();
                try {
                    Thread.sleep(0);                 //1000 milliseconds is one second.
                } catch(InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }

            Document cardDOM = Jsoup.parse(cardHtml);

            Elements rows = null;
            try{
                rows = cardDOM.getElementsByClass("cardtable").first().getElementsByClass("cardtablerow");
            }
            catch(NullPointerException e){
                continue; //card has no information to get, so we move to the next one
            }

            boolean foundFirstRow = false;
            for (Element row : rows) {
                Element header = row.getElementsByClass("cardtablerowheader").first();
                if (header == null) continue;
                String headerText = header.text();
                if (!foundFirstRow && !headerText.equals("Attribute") && !headerText.equals("Type") && !headerText.equals("Types")) {
                    continue;
                }
                if (headerText.equals("Other card information") || header.equals("External links")) {
                    // we have reached the end for some reasons, exit now
                    break;
                } else {
                    foundFirstRow = true;
                    Element dataElement = row.getElementsByClass("cardtablerowdata").first();
                    String data = dataElement.text();
                    switch(headerText){
                        case "Type" :   {
                            if(data.equals("Spell Card") || data.equals("Trap Card"))
                                card.setYcrSuperType(data);
                            else{
                                card.setYcrSuperType("Monster");
                                card.setYcrType(data);
                            }
                            break;
                        }
                        case "Property" :   {
                            card.setYcrIcon(data);
                            break;
                        }
                        case "Card effect types" :   {
                            card.setYcrCardEffectType(data);
                            break;
                        }
                        case "ATK/DEF" :   {
                            int atk = tryParse(data.split("/")[0]) == null ? -1 : tryParse(data.split("/")[0]);
                            int def = tryParse(data.split("/")[1]) == null ? -1 : tryParse(data.split("/")[1]);
                            card.setYcrAtk(atk);
                            card.setYcrDef(def);
                            break;
                        }
                        case "Level" :   {
                            card.setYcrLevel(tryParse(data) == null ? 0 : tryParse(data));
                            break;
                        }
                        case "Attribute" :   {
                            card.setYcrAttribute(data);
                            break;
                        }
                        case "Types" :   {
                            card.setYcrMonsterType(data);
                            break;
                        }
                        case "Pendulum Scale" :   {
                            card.setYcrPendulumScale(tryParse(data));
                            break;
                        }
                    }
                    //System.out.println(data);
                    if (headerText.equals("Card effect types") || headerText.equals("Limitation Text")) {
                        break;
                    }


                }
            }

            Element effectBox = cardDOM.getElementsByClass("cardtablespanrow").first().getElementsByClass("navbox-list").first();
            String effect = null;
            if(effectBox.toString().contains("Pendulum Effect")){
                effect = YgoWikiaHtmlCleaner.getCleanedHtml(effectBox);
                effect = Jsoup.clean(effect, Whitelist.none());

                String pEffect = effect.replaceFirst("Pendulum Effect", "");
                pEffect = pEffect.split("Monster Effect")[0].trim();
                effect = effect.split("Monster Effect")[1].trim();
                card.setYcrPendulumFlavor(pEffect);
                card.setYcrFlavorText(effect);
            }
            else {
                effect = YgoWikiaHtmlCleaner.getCleanedHtml(effectBox);
                card.setYcrFlavorText(effect);
            }

            //Element setInfo = cardDOM.getElementsByClass("wikitable").first();
            //boom! headshot!
            Element setInfo = cardDOM.select("table[class=wikitable sortable card-list]").first();
            if(setInfo != null) {
                Elements tmp = setInfo.select("td");
                //System.out.println(tmp.size());
                //various fixes
                if(tmp.size() == 5) tmp.remove(3);
                if(tmp.size() == 10){
                    tmp.remove(3);
                    tmp.remove(7);
                }
                for(int i = 0 ; i < tmp.size() ; i+=4){
                    String releaseDate = tmp.get(i).text();
                    String setId = tmp.get(i+1).text().split("-")[0].trim();
                    String setName = tmp.get(i+2).text();
                    card.setYcrRarity(tmp.get(i+3).text());

                    setId = insertSet(releaseDate, setId, setName);
                    card.setYcrSetID(setId);
                    card.setYcrImageName(grabCardImage(cardName, card.getYcrSetID(), cardDOM.getElementsByClass("cardtable-cardimage").first()));
                    cardsToInsert.add(new YgoCard(card));



                }
            }
            else{
                Element tmp = cardDOM.select("b:contains(TCG)").first().parent();
                tmp = tmp.getElementsByClass("navbox-list").first();
                Elements tmps = tmp.getElementsByTag("<p>");

                //edge case: first table is broken, move to second
                if(tmp.text() == null || tmp.text().equals("") || tmp.text().equals(" ")){
                    System.out.println("skipping " + cardName + " due to broken table");
                }
                else if(tmps.size() != 0){ //multiple sets
                    for(Element e : tmps){
                        String tmpStr = e.text();
                        String setName = tmpStr.split("(\\()")[0].trim();
                        //System.out.println(e.text());
                        tmpStr = tmpStr.split("(\\()")[1];
                        String setID = tmpStr.split("\\-")[0].trim();
                        tmpStr = tmpStr.split("\\- ")[1];

                        setID = insertSet("", setID, setName);
                        card.setYcrSetID(setID);
                        String cardRarity = tmpStr.split("\\)")[0].trim();
                        if(cardRarity.contains("/")){
                            String[] rarities = cardRarity.split("/");
                            for(String r : rarities){
                                card.setYcrRarity(r);
                                card.setYcrImageName(grabCardImage(cardName, card.getYcrSetID(), cardDOM.getElementsByClass("cardtable-cardimage").first()));
                                cardsToInsert.add(card); // card from same set has multiple rarities, needs to be inserted again with new rarity
                            }
                        }
                        else{
                            card.setYcrRarity(cardRarity);
                            card.setYcrImageName(grabCardImage(cardName, card.getYcrSetID(), cardDOM.getElementsByClass("cardtable-cardimage").first()));
                            cardsToInsert.add(card);
                        }


                    }
                }
                else{ //just one set
                    String tmpStr = tmp.text();
                    String setName = tmpStr.split("(\\()")[0].trim();
                    //System.out.println(e.text());
                    //edge case: rarity in parens
                    String setID = null;
                    String cardRarity = null;
                    if(tmpStr.split("\\(").length > 2){
                        String tmpRarity = tmpStr.split(("\\("))[2];
                        tmpStr = tmpStr.split("(\\()")[1];
                        setID = tmpStr.split("\\-")[0].trim();
                        cardRarity = tmpRarity.split("\\)")[0].trim();
                    }
                    else{
                        tmpStr = tmpStr.split("(\\()")[1];
                        setID = tmpStr.split("\\-")[0].trim();
                        tmpStr = tmpStr.split("\\- ")[1];
                        cardRarity = tmpStr.split("\\)")[0].trim();
                    }


                    setID = insertSet("", setID, setName);
                    card.setYcrSetID(setID);
                    if(cardRarity.contains("/")){
                        String[] rarities = cardRarity.split("/");
                        for(String r : rarities){
                            card.setYcrRarity(r);
                            card.setYcrImageName(grabCardImage(cardName, card.getYcrSetID(), cardDOM.getElementsByClass("cardtable-cardimage").first()));
                            cardsToInsert.add(card); // card from same set has multiple rarities, needs to be inserted again with new rarity
                        }
                    }
                    else{
                        card.setYcrRarity(cardRarity);
                        card.setYcrImageName(grabCardImage(cardName, card.getYcrSetID(), cardDOM.getElementsByClass("cardtable-cardimage").first()));
                        cardsToInsert.add(card);
                    }

                }

            }
        }
    }

    private String grabCardImage(String cardName, String setId, Element imageTable) {
        if(imageTable == null) return null; //no image

        String imageFolder = "Ygo" + File.separator + setId + File.separator;
        String imageName = null;
        Element imageRow = imageTable.getElementsByClass("cardtable-cardimage").first();
        String imageUrl = imageRow.select("a[href]").first().attr("abs:href");
        cardName = cardName.replaceAll("\"", "");
        cardName = cardName.replaceAll("\"", "");
        cardName = cardName.replaceAll("/", "");

        //Open a URL Stream
        Response resultImageResponse = null;
        try{
            resultImageResponse = Jsoup.connect(imageUrl).ignoreContentType(true).execute();
        }
        catch(IOException e){
            e.printStackTrace();
        }
        new File(imageFolder).mkdirs();
        File imageFile = new File(imageFolder + cardName + ".jpg");
        if(imageFile.isFile()){ //already exists
            imageName = new String(cardName);
            return imageName;
        }
        if(!imageFile.exists()) {
            try{
                imageFile.createNewFile();
            }catch(IOException e){
                e.printStackTrace();
            }

        }
        FileOutputStream out = null;
        try{
            out = (new FileOutputStream(new java.io.File(imageFolder + cardName + ".jpg")));
        }catch(IOException e){
            e.printStackTrace();
        }


        try{
            out.write(resultImageResponse.bodyAsBytes());
            out.close();
        }catch(IOException e){
            e.printStackTrace();
        }

        imageName = new String(cardName);
        return imageName;

    }

    private String insertSet(String date, String setID, String setName) {
        if(setRefTable.get(setID) != null){
            if(date.length() > 0 && setRefTable.get(setID).getYstReleaseDate().length() == 0 && setName.equalsIgnoreCase(setRefTable.get(setID).getYstName())){
                //todo: check if set dates differ, assign s to the set with the later date
                setRefTable.get(setID).setYstReleaseDate(date);
            }
            else if(date.length() > 0 && setRefTable.get(setID).getYstReleaseDate().length() > 0){
                DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                Date incomingDate = null;
                Date existingDate = null;
                try{
                    incomingDate = format.parse(date);
                    existingDate = format.parse(setRefTable.get(setID).getYstReleaseDate());
                }
                catch(ParseException p){
                    p.printStackTrace();
                }
                if(incomingDate.after(existingDate)){
                    setID += "S";//interim fix for sets that have the same name, konami has an arsehole division
                    setID = insertSet(date, setID, setName);//recursive call with new set name
                }
                else if(incomingDate.before(existingDate)){
                    setRefTable.get(setID).setYstIdentifier(setRefTable.get(setID).getYstIdentifier() + "S");
                    YgoSet s = new YgoSet();
                    s.setYstReleaseDate(date);
                    s.setYstName(setName);
                    s.setYstIdentifier(setID);
                    setRefTable.put(setID, s);

                }//otherwise don't insert the date, is the same set
            }
        }
        else{ //create a new set
            YgoSet s = new YgoSet();
            s.setYstReleaseDate(date);
            s.setYstIdentifier(setID);
            s.setYstName(setName);
            setRefTable.put(setID, s);
        }
        return setID;
    }

    public Integer tryParse(String text) {
        try {
            return new Integer(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
