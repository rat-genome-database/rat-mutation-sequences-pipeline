package edu.mcw.rgd.ratMutationSequences;

import edu.mcw.rgd.dao.impl.*;
import edu.mcw.rgd.datamodel.*;

import java.util.List;
import java.util.Random;

public class DAO {

    private final AssociationDAO adao = new AssociationDAO();
    private final RgdVariantDAO rdao = new RgdVariantDAO();
    private final GeneDAO gdao = new GeneDAO();
    private final StrainDAO sDAO = new StrainDAO();
    private final MapDAO mdao = new MapDAO();


    public String getConnection() throws Exception {
        return rdao.getConnectionInfo();
    }
    public int insertAssociation(Association assoc) throws Exception{
        return adao.insertAssociation(assoc);
    }

    public void insertVariant(RgdVariant var, String objStatus, int speciesType) throws Exception {
        rdao.insertVariant(var, objStatus, speciesType);
    }

    public int insertMapData(MapData md) throws Exception{
        return mdao.insertMapData(md);
    }

    public void updateVariant(RgdVariant v) throws Exception{
        rdao.updateVariant(v);
    }

    public int updateMapData(MapData md) throws Exception{
        return mdao.updateMapData(md);
    }
    public List<RgdVariant> getRgdVariantsByGeneId(int rgdId) throws Exception{
        return rdao.getVariantsFromGeneRgdId(rgdId);
    }

    public Gene getGene(int rgdId) throws Exception{
        return gdao.getGene(rgdId);
    }

    public Strain getStrain(int rgdId) throws Exception{
        return sDAO.getStrain(rgdId);
    }

    public List<Map> getMaps(int speciesTypeKey) throws Exception{
        return mdao.getMaps(speciesTypeKey);
    }

    public List<MapData> getMapData(int rgdId) throws Exception {
        return mdao.getMapData(rgdId);
    }

    public int getPrimaryRefAssembly(int species) throws Exception {
        return mdao.getPrimaryRefAssembly(species).getKey();
    }

    public void insertStrainAssociation(int strainRgdId, int varRgdId) throws Exception{
        adao.insertStrainAssociation(strainRgdId,varRgdId);
    }

    public List getStrainAssociations(int strainRgdId) throws Exception{
        return adao.getStrainAssociations(strainRgdId);
    }

}
