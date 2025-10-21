package gov.lanl.agg.helpers;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class PredictionResponse {

    double actualRecall;
    String[] archivesPredicted;

    public String[] getArchivesPredicted() {
        return archivesPredicted;
    }

    public double getActualRecall() {
        return actualRecall;
    }

    public void setArchivesPredicted(String[] archivesPredicted) {
        this.archivesPredicted = archivesPredicted;
    }

    public void setActualRecall(double actualRecall) {
        this.actualRecall = actualRecall;
    }

    public PredictionResponse(){}
}
