package edu.mcw.rgd.ratMutationSequences;

import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.datamodel.Map;
import edu.mcw.rgd.process.FastaParser;
import edu.mcw.rgd.process.Utils;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.io.FileInputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.apache.poi.ss.usermodel.CellType.BLANK;
import static org.apache.poi.ss.usermodel.CellType.STRING;

public class Manager {

    private String version;
    private static DAO dao = new DAO();
    protected Logger logger = LogManager.getLogger("status");
    private SimpleDateFormat sdt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws Exception
    {
        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));
        Manager manager = (Manager) (bf.getBean("manager"));

        manager.run();

    }

    void run() throws Exception{

        logger.info(getVersion());
        logger.info("   "+dao.getConnection());

        long pipeStart = System.currentTimeMillis();
        logger.info("Pipeline started at "+sdt.format(new Date(pipeStart))+"\n");

        File directory = new File("data/");
        File[] files = directory.listFiles(File::isFile);
        long lastModifiedTime = Long.MIN_VALUE;
        File chosenFile = null;

        if (files != null) {
            for (File file : files) {
                if (file.lastModified() > lastModifiedTime) {
                    chosenFile = file;
                    lastModifiedTime = file.lastModified();
                }
            }
        }

//            System.out.println(chosenFile.getName());
        OPCPackage pkg = null;
        try{
            DataFormatter formatter = new DataFormatter();
            pkg = OPCPackage.open(chosenFile);
            XSSFWorkbook workbook = new XSSFWorkbook(pkg);
            for (int ws = 0; ws < workbook.getNumberOfSheets(); ws++) {
                logger.info("\n");
                logger.info("\t============="+workbook.getSheetName(ws)+"=============");
                XSSFSheet sheet = workbook.getSheetAt(ws);
                Iterator<Row> rowIterator = sheet.iterator();
                List<excelLine> allelicVariants = new ArrayList<>();
                List<Map> ratMaps = dao.getMaps(3);
                int k = 0;
                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    if (k < 2 || checkIfRowIsEmpty(row)) {
                        k++;
                        continue;
                    }
                    // first 2 rows are about the columns
                    Iterator<Cell> cellIterator = row.cellIterator();
                    excelLine el = new excelLine();
                    MapData md = new MapData();
                    RgdVariant rv = new RgdVariant();
                    boolean isDeletion = false;
                    boolean isInsertion = false;
                    int i = 0;
                    for (i = 0; i < row.getPhysicalNumberOfCells(); i++) {
                        Cell cell = row.getCell(i);
                        if (cell == null)
                            continue;
                        switch (i) {
                            case 0: // col 0-3 are about strains
                                String strainId = formatter.formatCellValue(cell);
                                Strain s = dao.getStrain(Integer.parseInt(strainId));
                                el.setStrain(s);
                                break;
                            case 1:
                            case 2:
                            case 3:
                                String status = cell.getStringCellValue();
                                el.setStatus(status);
                                break;
                            case 4: // col 4-5 about allele
                                String id = formatter.formatCellValue(cell);
                                Gene g = dao.getGene(Integer.parseInt(id));
                                el.setAllele(g);
                                break;
                            case 5:
                                String tmp = cell.getStringCellValue().replace("<sup>", "");
                                String varSym = tmp.replace("</sup>", "");
                                rv.setName(varSym + "-var1");
                                break;
                            case 6:  // col 6-7 about variant, generate rgd_id and create name (remove <sup></sup> and add "-var1")
                                // variant rgd_id generated when entered
                                break;
                            case 7:
//                            if (cell.getCellType()==STRING)
//                            {
//                                String name = cell.getStringCellValue();
//                                if (!rv.getName().equals(name)){
//                                    rv.setName(name);
//                                }
//                            }
                                break;
                            case 8: // col 8-9 SO term name, SO:########; deletions say get reference, load second
                                String soTerm = cell.getStringCellValue().toLowerCase();
                                if (soTerm.equals("deletion"))
                                    isDeletion = true;
                                else if (soTerm.equals("insertion"))
                                    isInsertion = true;
                                else if (soTerm.equals("delins")) {
                                    isDeletion = true;
                                    isInsertion = true;
                                }
                                break;
                            case 9:
                                String accId = cell.getStringCellValue();
                                rv.setType(accId);
                                break;
                            case 10: // col 10 is assembly, remove "()" and get proper map key or check for "mRatBN7.2" and put 372
                                String asm = cell.getStringCellValue().replaceAll("[\\[\\](){}]", "");
                                String[] assemblies = asm.split("/");
                                md.setMapKey(getMapKey(assemblies[0], ratMaps));
                                rv.setSpeciesTypeKey(3);
                                break;
                            case 11: // col 11-13 position in chromosome, shave off "chr" and remove ',' in position
                                String chr = cell.getStringCellValue().substring(3);
                                md.setChromosome(chr);
                                md.setStrand("+");
                                md.setSrcPipeline("RGD");
                                break;
                            case 12:
                                String startStr = formatter.formatCellValue(cell).replace(",", "");
                                int start = Integer.parseInt(startStr);
                                md.setStartPos(start);
                                break;
                            case 13:
                                String stopStr = formatter.formatCellValue(cell).replace(",", "");
                                int stop = Integer.parseInt(stopStr);
                                md.setStopPos(stop);
                                break;
                            case 14: // col 14/15 use getRefAllele method and make changes, so it is not gwas
                                if (isDeletion) {
                                    String ref = getRefAllele(md.getMapKey(), md).replaceAll("[\\n\\r\\s]", "");
                                    rv.setRefNuc(ref.toUpperCase());
                                }
                                break;
                            case 15:
//                            if (cell.getCellType()==STRING){
//                                String refNuc = cell.getStringCellValue().toUpperCase();
//                                refNuc = refNuc.replace(" ","");
//
//                                if (refNuc.equals(rv.getRefNuc()))
//                                    rv.setRefNuc(refNuc);
//                            }
                                break;
                            case 16: // col 16 is variant... trim if needed? also toUpperCase
                                if (cell.getCellType() == BLANK)
                                    rv.setVarNuc(null);
                                else if (cell.getCellType() == STRING && isInsertion) {
                                    String varNuc = cell.getStringCellValue().toUpperCase();
                                    rv.setVarNuc(varNuc);
                                }
                                break;

                        }

                    }
                    el.setVariant(rv);
                    el.setMapData(md);
                    allelicVariants.add(el);
                    k++;
                }
                for (excelLine el : allelicVariants) {
                    List<RgdVariant> vars;
                    vars = dao.getRgdVariantsByGeneId(el.getAllele().getRgdId());
                    el.setExistingVars(vars);
                    String alleleDesc = "";
                    if (varExist(el)) {
                        // update RgdVariant
                        logger.info("\tRGD Variant object with no change: " + el.getVariant().getRgdId() + ", Var Name: " + el.getVariant().getName());
                        List sAssoc = dao.getStrainAssociations(el.getStrain().getRgdId());
                        boolean exist = false;
                        for (Object o : sAssoc) {
                            try {
                                int varRgdId = (int) o;
                                if (varRgdId == el.getVariant().getRgdId()) {
                                    exist = true;
                                }

                            } catch (Exception ignore) {
                                // if it is not an int, then it is a gene, sslp, or strain
                            }
                        }
                        if (!exist) {
                            logger.info("\t Adding strain association between strain:"+ el.getStrain().getRgdId() + " and variant:"+el.getVariant().getRgdId());
                            dao.insertStrainAssociation(el.getStrain().getRgdId(), el.getVariant().getRgdId());
                        }
                        String oldDesc;
                        if (Utils.isStringEmpty(el.getVariant().getDescription())) {
                            oldDesc = el.getVariant().getDescription();
                            if (!Utils.isStringEmpty(el.getAllele().getDescription())) {
                                alleleDesc = el.getAllele().getDescription().substring(0, 1).toLowerCase() + el.getAllele().getDescription().substring(1);
                                if (alleleDesc.startsWith("this allele") || alleleDesc.startsWith("the allele") || alleleDesc.startsWith("allele"))
                                    el.getVariant().setDescription("Variant associated with allele " + el.getAllele().getSymbol() + "; " + alleleDesc);
                                else
                                    el.getVariant().setDescription("Variant associated with allele " + el.getAllele().getSymbol() + "; the allele is " + alleleDesc);
                            } else {
                                el.getVariant().setDescription("Variant associated with allele " + el.getAllele().getSymbol());
                            }
                            if (!Utils.stringsAreEqual(oldDesc, el.getVariant().getDescription()))
                                dao.updateVariant(el.getVariant());
                        }

                    } else {
                        if (el.getConflict())
                            continue;
                        if (!Utils.isStringEmpty(el.getAllele().getDescription())) {
                            if (el.getAllele().getDescription().startsWith("CRISPR/Cas9") || el.getAllele().getDescription().startsWith("ZFN") || el.getAllele().getDescription().startsWith("TALEN"))
                                alleleDesc = el.getAllele().getDescription();
                            else
                                alleleDesc = el.getAllele().getDescription().substring(0, 1).toLowerCase() + el.getAllele().getDescription().substring(1);
                            if (alleleDesc.startsWith("this allele") || alleleDesc.startsWith("the allele") || alleleDesc.startsWith("allele"))
                                el.getVariant().setDescription("Variant associated with allele " + el.getAllele().getSymbol() + "; " + alleleDesc);
                            else
                                el.getVariant().setDescription("Variant associated with allele " + el.getAllele().getSymbol() + "; " + alleleDesc);
                        } else {
                            el.getVariant().setDescription("Variant associated with allele " + el.getAllele().getSymbol());
                        }
                        dao.insertVariant(el.getVariant(), el.getStatus(), el.getVariant().getSpeciesTypeKey());
                        logger.info("\tInserting variant, mapData, and association for RgdId: " + el.getVariant().getRgdId() + " Var Name: " + el.getVariant().getName());
                        el.getMapData().setRgdId(el.getVariant().getRgdId());
                        dao.insertMapData(el.getMapData());
                        // create association
                        Association a = new Association();
                        a.setAssocType("variant_to_gene");
                        a.setAssocSubType("allele");
                        a.setMasterRgdId(el.getVariant().getRgdId());
                        a.setDetailRgdId(el.getAllele().getRgdId());
                        dao.insertAssociation(a);
                        dao.insertStrainAssociation(el.getStrain().getRgdId(), el.getVariant().getRgdId());
                    }
                } // end excelLine for
            }// end workbook for

        }
        catch (Exception e){
            e.printStackTrace();
            logger.info(e);
        }
        finally {
            if (pkg!=null)
                pkg.close();
        }

        logger.info("\nrat-mutation-sequences pipeline runtime -- elapsed time: "+
                Utils.formatElapsedTime(pipeStart,System.currentTimeMillis()));

    }
    // replace GWASCatalog to new class
    String getRefAllele(int mapKey, MapData md) throws Exception {

        FastaParser parser = new FastaParser();
        parser.setMapKey(mapKey);
        if( parser.getLastError()!=null ) {

        }

        parser.setChr(Utils.defaultString(md.getChromosome()));

        int startPos = md.getStartPos();
        int stopPos = md.getStopPos();

        String fasta = parser.getSequence(startPos, stopPos);
        if( parser.getLastError()!=null ) {

        }
        if( fasta == null ) {
            return null;
        }

        return fasta;
    }

    boolean checkIfRowIsEmpty(Row row) {
        if (row == null) {
            return true;
        }
        if (row.getLastCellNum() <= 0) {
            return true;
        }
        for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
            Cell cell = row.getCell(cellNum);
            if (cell != null && cell.getCellType() != CellType.BLANK && !isBlankString(cell.toString())) {
                return false;
            }
        }
        return true;
    }

    boolean isBlankString(String string) {
        return string == null || string.trim().isEmpty();
    }

    Integer getMapKey(String assembly, List<Map> maps) throws Exception{
        for (Map m : maps){
            if (assembly.equals(m.getName()))
                return m.getKey(); // returns found map key
        }
        // if it cannot find assembly by name, convenient switch case
        switch (assembly){
            case "rn5":
            case "RGSC 5.0":
                return 70;
            case "rn6":
            case "RGSC 6.0":
                return 360;
            case "rn7":
            case "mRatBN7.2":
                return 372;
            default:
                return 372; // returns primary assembly
        }

    }

    public boolean varExist(excelLine el) throws Exception {
        // if exist in old assembly, leave it be.
        RgdVariant v = el.getVariant();
        MapData md = el.getMapData();
        for (RgdVariant var : el.getExistingVars()){
            String logMe ="";
            boolean mapExist = false;
            List<MapData> mapData = dao.getMapData(var.getRgdId());// check md if latest assembly exists, if not return false
            for (MapData m : mapData){
                if (m.getMapKey()==dao.getPrimaryRefAssembly(3)) {
                    logMe = "\n\t\t\tChecking mapdata for RGDID: " +var.getRgdId() +", "+var.getName() +
                            "\n\t\t\t\tIn DB; chromosome: " +m.getChromosome() + "\tstart:" + m.getStartPos()+"\tstop: "+m.getStopPos() +
                            "\n\t\t\t\tIn File; chromosome: " +md.getChromosome() + "\tstart:" + md.getStartPos()+"\tstop: "+md.getStopPos();
                    mapExist = true;
                }
            }
            if (!mapExist)
                return false;
            if (var.getName().equals(v.getName())){
                v.setRgdId(var.getRgdId());
//                v.setDescription(var.getDescription());
//                v.setNotes(var.getNotes());
                el.setVariant(v);
                if (Utils.stringsAreEqual(var.getRefNuc(),v.getRefNuc()) && Utils.stringsAreEqual(var.getVarNuc(),v.getVarNuc()))
                    return true;
                else {
                    logger.info("\t\t~~~~~~~~~~~CONFLICT WITH RGDID: "+var.getRgdId()+"~~~~~~~~~~~~~~~");
                    logger.info(logMe);
                    logger.info("\t\t\tChecking nucleotides for RGDID: " +var.getRgdId() + ", "+var.getName());
                    logger.info("\t\t\t\tIn DB; Reference: "+var.getRefNuc() +"\tVariant:" + var.getVarNuc() );
                    logger.info("\t\t\t\tIn File; Reference: "+v.getRefNuc() +"\tVariant:" + v.getVarNuc() );
                    logger.info("\n\t\t~~~~~~~~~~~CONFLICT WITH RGDID: "+var.getRgdId()+"~~~~~~~~~~~~~~~");
                    el.setConflict(true);
                }
                return false;
            }
        }
        return false;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
