package team.monroe.org.takeaway.presentations;

import java.io.Serializable;

public class Source implements AwarePath, Serializable {

    public final String id;
    public final String title;

    public Source(String id, String title) {
        this.id = id;
        this.title = title;
    }

    @Override
    public String getRelativePath() {
        return "";
    }

    @Override
    public Source getSource() {
        return this;
    }
}
