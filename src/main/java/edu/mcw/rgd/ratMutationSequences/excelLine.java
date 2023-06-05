package edu.mcw.rgd.ratMutationSequences;

import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.MapData;
import edu.mcw.rgd.datamodel.RgdVariant;
import edu.mcw.rgd.datamodel.Strain;

import java.util.List;

public class excelLine {
    private Strain strain;
    private Gene allele;
    private RgdVariant variant;
    private MapData mapData;
    private List<RgdVariant> existingVars;

    public Strain getStrain() {
        return strain;
    }

    public void setStrain(Strain strain) {
        this.strain = strain;
    }

    public Gene getAllele() {
        return allele;
    }

    public void setAllele(Gene allele) {
        this.allele = allele;
    }

    public RgdVariant getVariant() {
        return variant;
    }

    public void setVariant(RgdVariant variant) {
        this.variant = variant;
    }

    public MapData getMapData() {
        return mapData;
    }

    public void setMapData(MapData mapData) {
        this.mapData = mapData;
    }

    public List<RgdVariant> getExistingVars() {
        return existingVars;
    }

    public void setExistingVars(List<RgdVariant> existingVars) {
        this.existingVars = existingVars;
    }
}
