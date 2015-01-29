/**
 * Created by Jon on 1/12/2015.
 */
public class YgoSet {
    private String ystIdentifier;
    private String ystName;
    private String ystReleaseDate;
    private String ystImgPath;

    public String getYstIdentifier() {
        return ystIdentifier;
    }

    public void setYstIdentifier(String ystIdentifier) {
        this.ystIdentifier = ystIdentifier;
    }

    public String getYstName() {
        return ystName;
    }

    public void setYstName(String ystName) {
        this.ystName = ystName;
    }

    public String getYstReleaseDate() {
        return ystReleaseDate;
    }

    public void setYstReleaseDate(String ystReleaseDate) {
        this.ystReleaseDate = ystReleaseDate;
    }

    public String getYstImgPath() {
        return ystImgPath;
    }

    public void setYstImgPath(String ystImgPath) {
        this.ystImgPath = ystImgPath;
    }

    public String toString(){
        return String.format("%s,%s,%s", ystIdentifier, ystName, ystReleaseDate, ystImgPath);
    }
}
