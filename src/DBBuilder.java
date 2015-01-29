import com.mysql.jdbc.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;
import org.jsoup.Connection.Response;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;
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
    public HashMap<String, YgoSet> cardSetTable = null;
    public HashMap<YgoSet, Integer> setIDTable = null;
    public ArrayList<YgoCard> cardsToInsert = null;

    public void start(){
        String allCardsUrl = null;
        cardsToInsert = new ArrayList<>();
        cardLinkTable = new HashMap<>();
        setRefTable = new HashMap<>();
        cardSetTable = new HashMap<>();
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
/*
        try{
            Class.forName("com.mysql.jdbc.Driver");
        }
        catch (ClassNotFoundException c){
            c.printStackTrace();
        }
        Connection conn = null;
        try{
            setIDTable = new HashMap<>();
            conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/ygo_rebuild?user=generic&password=generic11PASSWORD");

            for(YgoSet y : setRefTable.values()){
                String sql = "INSERT INTO stt_yugioh_set (yst_name, yst_release_date, yst_path_name) VALUES (?,?,?) ";
                PreparedStatement stmt = null;
                try{
                    stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
                }
                catch(SQLException s){
                    s.printStackTrace();
                }

                System.out.println("Inserting " + y.getYstName());

                stmt.setString(1, y.getYstName());
                stmt.setString(2, y.getYstReleaseDate());
                stmt.setString(3, y.getYstImgPath());

                stmt.executeUpdate();
                ResultSet rs = stmt.getGeneratedKeys();
                int setRefId = -1;
                if (rs != null && rs.next()) {
                    setRefId = rs.getInt(1);
                }

                setIDTable.put(y, setRefId );
            }

            for(YgoCard y : cardsToInsert){
                String sql = "INSERT INTO stt_yugioh_card (ycr_name, ycr_super_type, ycr_set_id, ycr_rarity, ycr_type, " +
                             "ycr_attribute, ycr_card_effect_type, ycr_level, ycr_rank, ycr_atk, ycr_def, ycr_flavor_text, " +
                             "ycr_pendulum_scale, ycr_pendulum_flavor, ycr_image_name, ycr_icon, ycr_monster_type, ycr_card_id) " +
                             "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";
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
                stmt.setInt(3, setIDTable.get(cardSetTable.get(y.getYcrCardID())));
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
                stmt.setString(18, y.getYcrCardID());


                stmt.execute();
            }

            conn.close();
        }

        catch(SQLException s){
            s.printStackTrace();
        }*/


    }

    private void parseCardInformation(ArrayList<String> cardNames){
        String cardUrl = null;
        for(String cardName : cardNames) {
            //skip token, too lazy to parse
            if(cardName.equalsIgnoreCase("token")) continue;
            System.out.println("working on " + cardName);

            YgoCard card = new YgoCard();
            card.setYcrName(cardName);
            //cardUrl = "http://yugioh.wikia.com" + cardLinkTable.get(cardName);
            cardUrl = "http://yugioh.wikia.com/wiki/Vylon_Alpha";
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
                                card.setYcrMonsterType(data);
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
                        case "Rank" :   {
                            card.setYcrRank(tryParse(data) == null ? 0 : tryParse(data));
                            break;
                        }
                        case "Attribute" :   {
                            card.setYcrAttribute(data);
                            break;
                        }
                        case "Types" :   {
                            String[] typeInfo = data.split("/");
                            card.setYcrMonsterType(typeInfo[0]);
                            for(int i = 1; i < typeInfo.length; i++){
                                if(card.getYcrType() == null){
                                    card.setYcrType(typeInfo[i]);
                                }
                                else{

                                    card.setYcrType(card.getYcrType().concat("," + typeInfo[i]));
                                }
                            }

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

            //make all nulls monsters
            if(card.getYcrSuperType() == null) card.setYcrSuperType("Monster");

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

            //remove duplicates from effect field
            if(card.getYcrCardEffectType() != null){
                HashMap<String, String> doops = new HashMap<>();
                String[] effects = card.getYcrCardEffectType().split(",");
                if(effects != null){
                    card.setYcrCardEffectType(null);
                    for(String s : effects){
                        s = s.trim();
                        if(doops.get(s) == null){
                            doops.put(s,s);
                        }
                    }
                    for(String s : doops.values()){
                        if(card.getYcrCardEffectType() == null){
                            card.setYcrCardEffectType(s);
                        }
                        else{
                            card.setYcrCardEffectType(card.getYcrCardEffectType() + ',' + s);
                        }
                    }
                }
            }

            Element cardInfoSelect = cardDOM.select("b:contains(TCG)").first().parent();
            cardInfoSelect = cardInfoSelect.getElementsByClass("navbox-list").first();

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
                    String CardId = tmp.get(i+1).text();
                    String setName = tmp.get(i+2).text();
                    card.setYcrRarity(normalizeRarity(tmp.get(i+3).text()));
                    card.setYcrCardID(CardId);

                    cardSetTable.put(CardId, insertSet(releaseDate, setName));
                    card.setYcrImageName(grabCardImage(cardName, cardDOM.getElementsByClass("cardtable-cardimage").first(), setName, card));
                    cardsToInsert.add(new YgoCard(card));



                }
            }
            else if(cardInfoSelect.getElementsByTag("a").size()%3 == 0){
                Elements tmp = cardInfoSelect.getElementsByTag("a");
                for(int i = 0 ; i < tmp.size() ; i+=3){
                    String setName = tmp.get(i).text();
                    String cardId = tmp.get(i+1).text();
                    card.setYcrCardID(cardId);
                    card.setYcrRarity(normalizeRarity(tmp.get(i+2).text().trim()));

                    cardSetTable.put(cardId, insertSet("", setName));
                    card.setYcrImageName(grabCardImage(cardName,cardDOM.getElementsByClass("cardtable-cardimage").first(), setName, card));
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

                        insertSet("", setName);
                        String cardRarity = tmpStr.split("\\)")[0].trim();
                        if(cardRarity.contains("/")){
                            String[] rarities = cardRarity.split("/");
                            for(String r : rarities){
                                card.setYcrRarity(normalizeRarity(r));
                                card.setYcrImageName(grabCardImage(cardName,cardDOM.getElementsByClass("cardtable-cardimage").first(), setName, card));
                                cardsToInsert.add(new YgoCard(card)); // card from same set has multiple rarities, needs to be inserted again with new rarity
                            }
                        }
                        else{
                            card.setYcrRarity(normalizeRarity(cardRarity));
                            card.setYcrImageName(grabCardImage(cardName,cardDOM.getElementsByClass("cardtable-cardimage").first(), setName, card));
                            cardsToInsert.add(new YgoCard(card));
                        }


                    }
                }
                else{ //just one set(you'd think)
                    String tmpStr = tmp.text();
                    String setName = tmpStr.split("(\\()")[0].trim();
                    //System.out.println(e.text());
                    //edge case: rarity in parens
                    String setID = null;
                    String cardRarity = null;
                    boolean added = false;
                    //multiple sets, one set of parens around the card ID and rarity
                    if(tmpStr.split("\\)").length > 1){
                        for(int i = 0; i < tmpStr.split("\\)").length ; i++){
                            String tmpSetInfo = tmpStr.split("\\)")[i].trim();
                            setName = tmpSetInfo.split("\\(")[0].trim();
                            tmpSetInfo = tmpSetInfo.split("\\(")[1].trim();
                            String CardId = tmpSetInfo.split("-")[0].trim() + '-' + tmpSetInfo.split("-")[1].trim();
                            card.setYcrCardID(CardId);
                            cardRarity = tmpSetInfo.split("-")[2].trim();

                            cardSetTable.put(CardId, insertSet("", setName));

                            checkRarityAndInsert(cardRarity, card, cardName, cardDOM, setName);
                        }
                        added=true;
                    }
                    else if(tmpStr.split("\\(").length > 2){
                        String tmpRarity = tmpStr.split(("\\("))[2];
                        tmpStr = tmpStr.split("(\\()")[1];
                        String cardId = tmpStr.split("\\-")[0].trim() + '-' + tmpStr.split("\\-")[1].trim();
                        card.setYcrCardID(cardId);
                        cardRarity = tmpRarity.split("\\)")[0].trim();
                        cardSetTable.put(cardId, insertSet("", setName));
                    }
                    else{
                        tmpStr = tmpStr.split("(\\()")[1];
                        String cardId = tmpStr.split("\\-")[0].trim() + '-' + tmpStr.split("\\-")[1].trim();
                        card.setYcrCardID(cardId);
                        tmpStr = tmpStr.split("\\- ")[1];
                        cardRarity = tmpStr.split("\\)")[0].trim();
                        cardSetTable.put(cardId, insertSet("", setName));
                    }

                    if(!added)checkRarityAndInsert(cardRarity, card, cardName, cardDOM, setName);

                }

            }
        }
    }

    private void checkRarityAndInsert(String cardRarity, YgoCard card, String cardName, Document cardDOM, String setName) {
        if(cardsToInsert.contains(card))
            return;

        if(cardRarity.contains("/")){
            String[] rarities = cardRarity.split("/");
            for(String r : rarities){
                card.setYcrRarity(normalizeRarity(r));
                card.setYcrImageName(grabCardImage(cardName,cardDOM.getElementsByClass("cardtable-cardimage").first(), setName, card));
                cardsToInsert.add(new YgoCard(card)); // card from same set has multiple rarities, needs to be inserted again with new rarity
            }
        }
        else if(cardRarity.contains("\\")){
            String[] rarities = cardRarity.split("\\\\");
            for(String r : rarities){
                card.setYcrRarity(normalizeRarity(r));
                card.setYcrImageName(grabCardImage(cardName,cardDOM.getElementsByClass("cardtable-cardimage").first(), setName, card));
                cardsToInsert.add(new YgoCard(card)); // card from same set has multiple rarities, needs to be inserted again with new rarity
            }
        }
        else{
            card.setYcrRarity(normalizeRarity(cardRarity));
            card.setYcrImageName(grabCardImage(cardName, cardDOM.getElementsByClass("cardtable-cardimage").first(), setName, card));
            cardsToInsert.add(new YgoCard(card));
        }
    }

    //normalizes the card rarities
    private String normalizeRarity(String r) {

        //so we don't have to mess with multiple similar cases
        r = r.toLowerCase();
        switch(r){
            case "c" :{
                r = "Common";
                break;
            }
            case "common" :{
                r = "Common";
                break;
            }
            case "nr" :{
                r = "Normal Rare";
                break;
            }
            case "sp" :{
                r = "Short Print";
                break;
            }
            case "hfr" :{
                r = "Holofoil Rare";
                break;
            }
            case "r" :{
                r = "Rare";
                break;
            }
            case "sr" :{
                r = "Super Rare";
                break;
            }
            case "super rare" :{
                r = "Super Rare";
                break;
            }
            case "ur" :{
                r = "Ultra Rare";
                break;
            }
            case "ultra rare" :{
                r = "Ultra Rare";
                break;
            }
            case "utr" :{
                r = "Ultimate Rare";
                break;
            }
            case "gr" :{
                r = "Ghost Rare";
                break;
            }
            case "ghost rare" :{
                r = "Ghost Rare";
                break;
            }
            case "hgr" :{
                r = "Holographic Rare";
                break;
            }
            case "plr" :{
                r = "Platinum Rare";
                break;
            }
            case "platinum rare" :{
                r = "Platinum Rare";
                break;
            }
            case "secret rare" :{
                r = "Secret Rare";
                break;
            }
            case "scr" :{
                r = "Secret Rare";
                break;
            }
            case "pscr" :{
                r = "Prismatic Secret Rare";
                break;
            }
            case "uscr" :{
                r = "Ultra Secret Rare";
                break;
            }
            case "ultra secret rare" :{
                r = "Ultra Secret Rare";
                break;
            }
            case "scur" :{
                r = "Secret Ultra Rare";
                break;
            }
            case "escr" :{
                r = "Extra Secret Rare";
                break;
            }
            case "plscr" :{
                r = "Platinum Secret Rare";
                break;
            }
            case "platinum secret rare" :{
                r = "Platinum Secret Rare";
                break;
            }
            case "npr" :{
                r = "Normal Parallel Rare";
                break;
            }
            case "normal parallel rare" :{
                r = "Normal Parallel Rare";
                break;
            }
            case "spr" :{
                r = "Super Parallel Rare";
                break;
            }
            case "upr" :{
                r = "Ultra Parallel Rare";
                break;
            }
            case "ultra parallel rare" :{
                r = "Ultra Parallel Rare";
                break;
            }
            case "sfr" :{
                r = "Starfoil Rare";
                break;
            }
            case "msr" :{
                r = "Mosaic Rare";
                break;
            }
            case "mosaic rare" :{
                r = "Mosaic Rare";
                break;
            }
            case "shr" :{
                r = "Shatterfoil Rare";
                break;
            }
            case "shatterfoil rare" :{
                r = "Shatterfoil Rare";
                break;
            }
            case "cr" :{
                r = "Collector's Rare";
                break;
            }
            case "mr" :{
                r = "Millenium Rare";
                break;
            }
            case "gur" :{
                r = "Gold Rare";
                break;
            }
            case "gold rare" :{
                r = "Gold Rare";
                break;
            }
            case "gscr" :{
                r = "Gold Secret Rare";
                break;
            }
            case "gold secret rare" :{
                r = "Gold Secret Rare";
                break;
            }
            case "ggr" :{
                r = "Ghost-Gold Rare";
                break;
            }
            case "ghost\\gold rare" :{
                r = "Ghost-Gold Rare";
                break;
            }
            case "ghost/gold rare" :{
                r = "Ghost-Gold Rare";
                break;
            }
            case "dnpr" :{
                r = "Duel Terminal Normal Parallel Rare";
                break;
            }
            case "duel terminal normal parallel rare" :{
                r = "Duel Terminal Normal Parallel Rare";
                break;
            }
            case "dnrpr" :{
                r = "Duel Terminal Normal Rare Parallel Rare";
                break;
            }
            case "drpr" :{
                r = "Duel Terminal Rare Parallel Rare";
                break;
            }
            case "duel terminal rare parallel rare" :{
                r = "Duel Terminal Rare Parallel Rare";
                break;
            }
            case "dspr" :{
                r = "Duel Terminal Super Parallel Rare";
                break;
            }
            case "dupr" :{
                r = "Duel Terminal Ultra Parallel Rare";
                break;
            }
            case "dscrp" :{
                r = "Duel Terminal Secret Parallel Rare";
                break;
            }
            case "se" :{
                r = "Standard Edition";
                break;
            }
            case "1e" :{
                r = "1st Edition";
                break;
            }
            case "ue" :{
                r = "Unilimited Edition";
                break;
            }
            case "le" :{
                r = "Limited Edition";
                break;
            }
            case "dt" :{
                r = "Duel Terminal Edition";
                break;
            }
            case "fr" :{
                r = "Fixed Rarity";
                break;
            }
            case "rp" :{
                r = "Replica";
                break;
            }
            case "gc" :{
                r = "Giant Card";
                break;
            }
            case "op" :{
                r = "Official Proxy";
                break;
            }
            case "ct" :{
                r = "Case Topper";
                break;
            }
            case "osp" :{
                r = "Oversized Promo";
                break;
            }
            case "bam" :{
                r = "BAM Legend";
                break;
            }
            default:{ //hack for specific sets
                if(r.split("-").length == 3) {
                    String tmp = r.split("-")[2].trim();
                    r = normalizeRarity(tmp);
                }
                break;
            }

        }
        return r;
    }

    private String grabCardImage(String cardName, Element imageTable, String setID, YgoCard card) {
        /*if(imageTable == null){
            String imageName = new String(cardName);
            imageName = imageName.replaceAll("\"", "");
            imageName = imageName.replace("\\", "");
            imageName = imageName.replaceAll("/", "");
            imageName = imageName.replaceAll(":", "");
            return imageName;
        }

        setID = setID.replaceAll("\"", "");
        setID = setID.replace("\\", "");
        setID = setID.replaceAll("/", "");
        setID = setID.replaceAll(":", "");

        String imageFolder = "C:\\Users\\Jon\\Desktop\\Ygo" + File.separator + setID + File.separator;
        String imageName = null;
        Element imageRow = imageTable.getElementsByClass("cardtable-cardimage").first();
        String imageUrl = imageRow.select("a[href]").first().attr("abs:href");
        imageName = cardName.replaceAll("\"", "");
        imageName = imageName.replace("\\", "");
        imageName = imageName.replaceAll("/", "");
        imageName = imageName.replaceAll(":", "");

        //Open a URL Stream
        Response resultImageResponse = null;
        try{
            resultImageResponse = Jsoup.connect(imageUrl).ignoreContentType(true).execute();
        }
        catch(IOException e){
            e.printStackTrace();
        }
        new File(imageFolder).mkdirs();
        File imageFile = new File(imageFolder + imageName + ".jpg");
        if(imageFile.isFile()){ //already exists
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
            out = (new FileOutputStream(new java.io.File(imageFolder + imageName + ".jpg")));
        }catch(IOException e){
            e.printStackTrace();
        }


        try{
            out.write(resultImageResponse.bodyAsBytes());
            out.close();
        }catch(IOException e){
            e.printStackTrace();
        }
        */
        String imageName = new String(cardName);

        Element imageRow = imageTable.getElementsByClass("cardtable-cardimage").first();
        String imageUrl = imageRow.select("a[href]").first().attr("abs:href");
        imageName = imageName.replaceAll("\"", "");
        imageName = imageName.replace("\\", "");
        imageName = imageName.replaceAll("/", "");
        imageName = imageName.replaceAll(":", "");
        imageName = imageName.replaceAll("\\?", "");
        //imageName = imageName.replaceAll("!", "");
        imageName += ' ' + card.getYcrCardID() + '-' + card.getYcrRarity();
        imageName = imageName.toLowerCase();
        imageName = imageName.replaceAll("\\?", "x");

        String imgPath = setID.replaceAll("\"", "");
        imgPath = imgPath.replace("\\", "");
        imgPath = imgPath.replaceAll("/", "");
        imgPath = imgPath.replaceAll(":", "");
        imgPath = imgPath.replaceAll("\\?", "");
        //imgPath = imgPath.replaceAll("!", "");
        imgPath = imgPath.toLowerCase();
        String imageFolder = "C:\\Users\\Jon\\Desktop\\Ygo" + File.separator + imgPath + File.separator;
        new File(imageFolder).mkdirs(); //to create the directory if it doesn't exist;
        File imageFile = new File(imageFolder + imageName + ".jpg");
        if(imageFile.isFile() || imageUrl == null || imageUrl.equals("")){ //already exists or image not found on site
            if(setRefTable.get(setID).getYstImgPath() == null) setRefTable.get(setID).setYstImgPath(imgPath);
            return imageName + ".jpg";
        }

        BufferedImage image = null;
        URL url = null;
        try{
            url = new URL(imageUrl);
        }
        catch(MalformedURLException e){
            e.printStackTrace();
        }


        try{
            image = ImageIO.read(url);

            ImageIO.write(image, "jpg",new File(imageFolder + imageName + ".jpg"));
        }
        catch(IOException e){
            e.printStackTrace();
        }
        if(setRefTable.get(setID).getYstImgPath() == null) setRefTable.get(setID).setYstImgPath(imgPath);

        return imageName + ".jpg";

    }

    private YgoSet insertSet(String date, String setName) {
        YgoSet y = new YgoSet();
        String setID = new String(setName);
        if(setRefTable.get(setID) != null){
            if(date.length() > 0 && setRefTable.get(setID).getYstReleaseDate().length() == 0 && setName.equalsIgnoreCase(setRefTable.get(setID).getYstName())){
                setRefTable.get(setID).setYstReleaseDate(date);
                y = setRefTable.get(setID);
            }
            else{
                y = setRefTable.get(setID);
            }
        }
        else{ //create a new set
            y.setYstReleaseDate(date);
            y.setYstIdentifier(setID);
            y.setYstName(setName);
            setRefTable.put(setID, y);
        }
        return y;
    }

    public Integer tryParse(String text) {
        try {
            return new Integer(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
