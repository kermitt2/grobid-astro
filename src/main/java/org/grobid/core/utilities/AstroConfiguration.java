package org.grobid.core.utilities;

import org.grobid.core.utilities.GrobidConfig.ModelParameters;

public class AstroConfiguration {

    public String corpusPath;
    public String templatePath;
    public String grobidHome;

    public ModelParameters model;

    public String getCorpusPath() {
        return this.corpusPath;
    }

    public void setCorpusPath(String corpusPath) {
        this.corpusPath = corpusPath;
    }

    public String getTemplatePath() {
        return this.templatePath;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    public String getGrobidHome() {
        return this.grobidHome;
    }

    public void setGrobidHome(String grobidHome) {
        this.grobidHome = grobidHome;
    }

    public ModelParameters getModel() {
        return model;
    }

    public void getModel(ModelParameters model) {
        this.model = model;
    }
}
