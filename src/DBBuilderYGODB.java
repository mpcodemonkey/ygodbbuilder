import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Jon on 1/24/2015.
 */
public class DBBuilderYGODB {
    public HashMap<String, String> cardLinkTable = null;
    public HashMap<String, YgoSet> setRefTable = null;
    public HashMap<String, YgoSet> cardSetTable = null;
    public HashMap<YgoSet, Integer> setIDTable = null;
    public ArrayList<YgoCard> cardsToInsert = null;
    public void start(){

        setRefTable = new HashMap<>();
        cardSetTable = new HashMap<>();
        cardsToInsert = new ArrayList<>();

        //all current id's of cards in the yugioh db
        String allCardsUrl = "http://www.db.yugioh-card.com/yugiohdb/card_search.action?ope=2&cid=";
        for(int i = 2000; i < 3000; i++){
            String cardUrl = allCardsUrl + i;

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

            //get the cars html as a document
            Document cardDOM = Jsoup.parse(cardHtml);

            //all cards have a name, links without card titles don't exist
            if(cardDOM.getElementById("broad_title") == null)continue;
            Element cardInfo = cardDOM.getElementsByTag("article").first();
            parseCardAndSetAttribs(cardInfo);


        }
        buildDatabase();
    }

    private void buildDatabase() {
        try{
            Class.forName("com.mysql.jdbc.Driver");
        }
        catch (ClassNotFoundException c){
            c.printStackTrace();
        }
        Connection conn = null;
        try{
            setIDTable = new HashMap<>();
            conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/dbyugiohdatabase?user=generic&password=generic11PASSWORD");

            for(YgoSet y : setRefTable.values()){
                String sql = "INSERT IGNORE INTO stt_yugioh_set (yst_name, yst_release_date, yst_path_name) VALUES (?,?,?) ";
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
                String sql = "INSERT IGNORE INTO stt_yugioh_card (ycr_name, ycr_super_type, ycr_set_id, ycr_rarity, ycr_type, " +
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
        }
    }

    private void parseCardAndSetAttribs(Element cardInfo){
        String name, supertype;
        name = supertype = null;

        YgoCard y = new YgoCard();

        name = cardInfo.getElementById("broad_title").text();

        y.setYcrName(name);
        Element tmp = cardInfo.getElementById("details");
        Elements attributes = tmp.getElementsByClass("item_box_title");
        if(attributes.first().getElementsByClass("item_box_title").first().text().equalsIgnoreCase("Attribute")) {
            supertype = "Monster";
            Element mon = attributes.get(0);
            String attribute = mon.text().replace("Attribute", "").trim();
            mon = attributes.get(1).parent();
            if(mon.text().contains("Level")){
                String level = mon.text().replace("Level", "").trim();
                y.setYcrLevel(tryParse(level));
            }
            else if(mon.text().contains("Rank")){
                String rank = mon.text().replace("Rank", "").trim();
                y.setYcrRank(tryParse(rank));
            }

            mon = attributes.get(2).parent();

            y.setYcrSuperType(supertype);
            y.setYcrAttribute(attribute);

            //pendulum monster, handle differently
            if(mon.text().contains("Pendulum")){
                String pendulumscale = mon.text().replace("Pendulum Scale", "").trim();
                mon = attributes.get(3).parent();
                String pendulumeffect = mon.text().replace("Pendulum Effect", "").trim();
                mon = attributes.get(4).parent();
                String monstertype = mon.text().replace("Monster Type", "").trim();
                mon = attributes.get(5).parent();
                String cardtype = mon.text().replace("Card Type", "").trim();
                mon = attributes.get(6).parent();
                String attack = mon.text().replace("ATK", "").trim();
                mon = attributes.get(7).parent();
                String defense = mon.text().replace("DEF", "").trim();
                mon = attributes.get(8).parent();
                String cardtext = mon.text().replace("Card Text", "").trim();

                y.setYcrPendulumScale(tryParse(pendulumscale));
                y.setYcrPendulumFlavor(pendulumeffect);
                y.setYcrMonsterType(monstertype);
                y.setYcrType(cardtype);
                if(tryParse(attack) != null){
                    y.setYcrAtk(tryParse(attack));
                }
                if(tryParse(defense) != null){
                    y.setYcrDef(tryParse(defense));
                }
                y.setYcrFlavorText(cardtext);

            }
            else{//run of the mill monster
                String monstertype = mon.text().replace("Monster Type", "").trim();
                mon = attributes.get(3).parent();
                String cardtype = mon.text().replace("Card Type", "").trim();
                mon = attributes.get(4).parent();
                String attack = mon.text().replace("ATK", "").trim();
                mon = attributes.get(5).parent();
                String defense = mon.text().replace("DEF", "").trim();
                mon = attributes.get(6).parent();
                String cardtext = mon.text().replace("Card Text", "").trim();

                y.setYcrMonsterType(monstertype);
                y.setYcrType(cardtype);

                if(tryParse(attack) != null){
                    y.setYcrAtk(tryParse(attack));
                }
                if(tryParse(defense) != null){
                    y.setYcrDef(tryParse(defense));
                }

                y.setYcrFlavorText(cardtext);

            }
        }
        else{
            Element ts = attributes.get(0).parent();
            String cardicon = ts.text().replace("Icon", "").trim().split(" ")[0].trim();
            supertype = ts.text().replace("Icon", "").trim().split(" ")[1].trim();
            ts = attributes.get(1).parent();
            String cardtext = ts.text().replace("Card Text", "").trim();

            y.setYcrSuperType(supertype);
            y.setYcrIcon(cardicon);
            y.setYcrFlavorText(cardtext);
        }

        Element setinfo = cardInfo.getElementById("pack_list");
        Elements sets = setinfo.getElementsByClass("row");
        for(Element e : sets){
            YgoSet ygoSet = new YgoSet();
            Elements imgs = e.select("img");
            String rarity = null;
            if(imgs.size() != 0){
                rarity = imgs.first().attr("alt");
            }
            else{
                rarity = "N/A";
            }
            Elements setfields = e.getElementsByTag("td");
            ygoSet.setYstReleaseDate(setfields.get(0).text());
            y.setYcrCardID(setfields.get(1).text());
            ygoSet.setYstName(setfields.get(2).text());

            y.setYcrRarity(rarity);

            cardSetTable.put(y.getYcrCardID(), insertSet(ygoSet.getYstReleaseDate(), ygoSet.getYstName()));
            cardsToInsert.add(new YgoCard(y));

            System.out.printf("Inserting %s with ID %s into set %s with rarity %s\n", y.getYcrName(), y.getYcrCardID(), ygoSet.getYstName(), y.getYcrRarity());
        }

        //System.out.println(y.toString());
    }

    public Integer tryParse(String text) {
        try {
            return new Integer(text);
        } catch (NumberFormatException e) {
            return null;
        }
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

}
