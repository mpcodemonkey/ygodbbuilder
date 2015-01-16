/**
 * Created by Jon on 1/12/2015.
 */
public class YgoCard {
    private String ycrName;
    private String ycrSuperType; //monster, trap, spell
    private String ycrSetID;
    private String ycrRarity;
    private String ycrType;
    private String ycrAttribute;
    private String ycrCardEffectType;
    private int ycrLevel;
    private int ycrRank;
    private int ycrAtk;
    private int ycrDef;
    private String ycrFlavorText;
    private int ycrPendulumScale;
    private String ycrPendulumFlavor;
    private String ycrImageName;
    private String ycrIcon;
    private String ycrMonsterType;

    public String getYcrName() {
        return ycrName;
    }

    public void setYcrName(String ycrName) {
        this.ycrName = ycrName;
    }

    public String getYcrSuperType() {
        return ycrSuperType;
    }

    public void setYcrSuperType(String ycrSuperType) {
        this.ycrSuperType = ycrSuperType;
    }

    public String getYcrSetID() {
        return ycrSetID;
    }

    public void setYcrSetID(String ycrSetID) {
        this.ycrSetID = ycrSetID;
    }

    public String getYcrRarity() {
        return ycrRarity;
    }

    public void setYcrRarity(String ycrRarity) {
        this.ycrRarity = ycrRarity;
    }

    public String getYcrType() {
        return ycrType;
    }

    public void setYcrType(String ycrType) {
        this.ycrType = ycrType;
    }

    public String getYcrAttribute() {
        return ycrAttribute;
    }

    public void setYcrAttribute(String ycrAttribute) {
        this.ycrAttribute = ycrAttribute;
    }

    public int getYcrLevel() {
        return ycrLevel;
    }

    public void setYcrLevel(int ycrLevel) {
        this.ycrLevel = ycrLevel;
    }

    public int getYcrRank() {
        return ycrRank;
    }

    public void setYcrRank(int ycrRank) {
        this.ycrRank = ycrRank;
    }

    public int getYcrAtk() {
        return ycrAtk;
    }

    public void setYcrAtk(int ycrAtk) {
        this.ycrAtk = ycrAtk;
    }

    public int getYcrDef() {
        return ycrDef;
    }

    public void setYcrDef(int ycrDef) {
        this.ycrDef = ycrDef;
    }

    public String getYcrFlavorText() {
        return ycrFlavorText;
    }

    public void setYcrFlavorText(String ycrFlavorText) {
        this.ycrFlavorText = ycrFlavorText;
    }

    public int getYcrPendulumScale() {
        return ycrPendulumScale;
    }

    public void setYcrPendulumScale(int ycrPendulumScale) {
        this.ycrPendulumScale = ycrPendulumScale;
    }

    public String getYcrPendulumFlavor() {
        return ycrPendulumFlavor;
    }

    public void setYcrPendulumFlavor(String ycrPendulumFlavor) {
        this.ycrPendulumFlavor = ycrPendulumFlavor;
    }

    public String getYcrImageName() {
        return ycrImageName;
    }

    public void setYcrImageName(String ycrImageName) {
        this.ycrImageName = ycrImageName;
    }

    public String getYcrIcon() {
        return ycrIcon;
    }

    public void setYcrIcon(String ycrIcon) {
        this.ycrIcon = ycrIcon;
    }

    public String getYcrMonsterType() {
        return ycrMonsterType;
    }

    public void setYcrMonsterType(String ycrMonsterType) {
        this.ycrMonsterType = ycrMonsterType;
    }

    public String getYcrCardEffectType() {
        return ycrCardEffectType;
    }

    public void setYcrCardEffectType(String ycrCardEffectType) {
        this.ycrCardEffectType = ycrCardEffectType;
    }

    public String toString(){
        return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                          ycrName, ycrSuperType, ycrSetID, ycrRarity, ycrType, ycrAttribute,
                          ycrCardEffectType, ycrLevel, ycrRank, ycrAtk, ycrDef, ycrFlavorText,
                          ycrPendulumScale, ycrPendulumFlavor, ycrImageName, ycrIcon, ycrMonsterType);
    }

    public YgoCard(){}
    public YgoCard(YgoCard that){
        this.ycrName = new String(that.ycrName);
        this.ycrSuperType = that.ycrSuperType == null ? null : new String(that.ycrSuperType);
        this.ycrSetID = that.ycrSetID == null ? null : new String(that.ycrSetID);
        this.ycrRarity = that.ycrRarity == null ? null : new String(that.ycrRarity);
        this.ycrType = that.ycrType == null? null : new String(that.ycrType);
        this.ycrAttribute = that.ycrAttribute == null ? null : new String(that.ycrAttribute);
        this.ycrCardEffectType = that.ycrCardEffectType == null ? null : new String(that.ycrCardEffectType);
        this.ycrLevel = that.ycrLevel;
        this.ycrRank = that.ycrRank;
        this.ycrAtk = that.ycrAtk;
        this.ycrDef = that.ycrDef;
        this.ycrFlavorText =  that.ycrFlavorText == null ? null : new String(that.ycrFlavorText);
        this.ycrPendulumScale = that.ycrPendulumScale;
        this.ycrPendulumFlavor = that.ycrPendulumFlavor == null ? null : new String(that.ycrPendulumFlavor);
        this.ycrImageName = that.ycrImageName == null ? null : new String(that.ycrImageName);
        this.ycrIcon = that.ycrIcon == null ? null : new String(that.ycrIcon);
        this.ycrMonsterType = that.ycrMonsterType == null ? null : new String(that.ycrMonsterType);
    }
}

