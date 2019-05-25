package logic.Calculations;

import data.DataFacade;
import data.DataFacadeImpl;
import data.exceptions.AlgorithmException;
import data.exceptions.DataException;
import data.models.MaterialModel;
import data.models.OrderModel;
import data.models.PartslistModel;

/**
 * This class handles materials needed for a flat roof on the carport.
 *
 * It is meant to return a PartslistModel with the items, which are then
 * appended to the 'master list' ("bill of materials").
 *
 * @see PartslistModel
 *
 * @author
 *
 * Material used for creation:
 * https://datsoftlyngby.github.io/dat2sem2019Spring/Modul4/Fog/CP01_DUR.pdf
 *
 * Material rules:
 *
 * Spær: Every 50cm
 *
 * Stern: 2 boards per side of the carport, except the back where we only need
 * 1. (length dependant)
 *
 * Vandbrædt: 3x (1x front, 1x each side, 0x back) (length dependant)
 *
 * Universalbeslag: 2x per spær. (Right/Left orientation) (9 screws per beslag)
 *
 * Hulbånd: 1x (10m). 2 screws pr. spær it crosses. (length dependant)
 *
 * !Plastic roofing:
 *
 * !Outermost parts must extend 5cm beyond its Spær to account for water
 * drainage.
 *
 * !Roof tiles have a 2 'wave' overlay (20cm) & 12 screws per cm^2.
 *
 * Tagpap roofing: Size dependant calculation
 *
 * Lægter: To be determined (only applied to felt roof (tagpap))
 *
 */
public class RoofFlatCalc
{

    //<editor-fold defaultstate="collapsed" desc="Algorithm Rules">
    private final int plasticRoofExtensionStandard = 50; //5cm extension beyond carport length
    private final int plasticRoofOverlapStandard = 100; //20cm overlap between two tiles means 10cm PER tile
    private final int plastictileScrewsStandard = 12;
    private final int rafterWidthStandard = 500; //1 rafter per 500mm (50cm)
    private final int rafterFittingScrewStandard = 9;
    private final int bargeboardScrewStandard = 4;
    private final int bandScrewStandard = 2;
    private final String helptext = "roof"; // Used to fetch the right helptext from database.
    // </editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Database material IDs">
    /* (height|width|length)*/
    private final int plasticTileSmallID = 29; //0x109x3600
    private final int plasticTileLargeID = 28; //0x109x6000
    private final int plasticTileScrewID = 22; //200 pcs. a pack
    private final int bandScrewsID = 21; //250 pcs. a pack
    private final int bandID = 23; //0x0x10000
    private final int fittingScrewsID = 21; //250 pcs. a pack
    private final int fittingLeftID = 16;
    private final int fittingRightID = 15;
    private final int rafterSmallID = 5; // 45x195x4800 (UNUSED CUSTOM ITEM (OUTSIDE SCOPE))
    private final int rafterLargeID = 54; //45x195x6000
    private final int fasciaWidthBottomID = 55; //25x200x3600
    private final int fasciaLengthBottomID = 56; //25x200x5400
    private final int fasciaWidthTopID = 57; //25x125x3600
    private final int fasciaLengthTopID = 58; //25x125x5400
    private final int bargeboardLengthID = 59;
    private final int bargeboardWidthID = 60;
    private final int bargeboardScrewsID = 20; //200 a pack
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Inner class calculation variables">
    /* Calculations */
    private int amountOfScrews; //total amount of screws needed
    private int rafterCount;
    //</editor-fold>

    /* Imports */
    private final DataFacade DAO; //data accessor

    public RoofFlatCalc()
    {
        rafterCount = 0;
        amountOfScrews = 0;
        this.DAO = DataFacadeImpl.getInstance();
    }

    /**
     * Master calculation; takes use of other methods to calculate all the parts
     * needed for a flat roof.
     *
     * @param order
     * @return
     * @throws data.exceptions.AlgorithmException
     * @throws DataException
     */
    public PartslistModel calculateFlatRoofStructure(OrderModel order) throws DataException, AlgorithmException
    {
        PartslistModel roofMaterials = new PartslistModel(); //items to be returned to master list
        /* calculate always needed (independent) items */
        roofMaterials.addPartslist(calculateMainParts(order));
        /* calculate items based on type of roof tile */
        roofMaterials.addPartslist(calculateDependantParts(order));

        roofMaterials.setRafterCount(rafterCount); //needed for svg
        return roofMaterials;
    }

    /**
     *
     * Calculates main parts that are generally always needed for the flat roof
     * part of the construction.
     *
     * @param order order in question
     *
     * @return returns partslistmodel of main parts
     * @throws DataException
     */
    protected PartslistModel calculateMainParts(OrderModel order) throws DataException
    {
        /* Initialize partslist to return */
        PartslistModel mainMaterials = new PartslistModel();

        /* Calculate amount of materials needed */
        PartslistModel rafters = calculateRafters(order);
        PartslistModel fascias = calculateFascias(order);
        PartslistModel bargeboards = calculateBargeboard(order);
        PartslistModel bands = calculateBand(order);
        PartslistModel fittings = calculateFittings(rafters);

        /* Add items to partslist */
        mainMaterials.addPartslist(rafters);
        mainMaterials.addPartslist(fascias);
        mainMaterials.addPartslist(bargeboards);
        mainMaterials.addPartslist(bands);
        mainMaterials.addPartslist(fittings);

        /* Return partslist*/
        return mainMaterials;
    }

    /**
     * calculates rafter ("spær") for carport roof
     *
     * first we calculate one row of rafters needed for carport width.
     *
     * then we find out how many rows we need.
     *
     * this gives us the total amount of rafters needed.
     *
     * @param order order in question
     * @return returns partlistmodel of items needed
     * @throws DataException
     */
    protected PartslistModel calculateRafters(OrderModel order) throws DataException
    {
        /* Set up return <partslistmodel>*/
        PartslistModel rafters = new PartslistModel();

        /* Get MaterialModel to return */
        MaterialModel rafter = DAO.getMaterial(rafterLargeID, helptext); //45x195x6000

        /* Set up variables */
        int width = order.getWidth();
        int length = order.getLength();
        int rafterLength = rafter.getLength();

        /* Calculation begin */
        //Lets calculate a row first. How many rafters to cover roof width?
        double rafterWidthCount = (width / rafterLength); //amount of large rafters to cover roof width

        double rafterWidthRemainder = (width % rafterLength);
        rafterWidthRemainder /= rafterLength; //any remaining rafters (i.e. at half length)?

        if (rafterWidthRemainder > 0)
        {
            rafterWidthRemainder = Math.ceil(rafterWidthRemainder); //lets round up in case of decimals
            rafterWidthCount += (int) rafterWidthRemainder; //this is the total amount of rafters for full width coverage.
        }
        /* Now we need to calculate amount of rows needed for the whole roof */
        int rafterTotalAmount = (length / rafterWidthStandard) * (int) rafterWidthCount; //we need to take the rule into account: 1 rafter pr 500mm
        double rafterLengthRemainder = (length % rafterWidthStandard) / rafterLength;

        if (rafterLengthRemainder > 0) //if there is less than 500mm to the end of the roof, add another rafter.
        {
            rafterLengthRemainder = Math.ceil(rafterLengthRemainder);
            rafterTotalAmount += (int) rafterLengthRemainder;
        }

        /* Update quantities */
        rafter.setQuantity(rafterTotalAmount);
        rafterCount = rafterTotalAmount; //needed for SVG

        /* Update price */
        rafter.setPrice(rafter.getQuantity() * rafter.getPrice());

        /* Update helptext */
        rafter.setHelptext("Spær, monteres på rem");

        /* Add to <partslistmodel> */
        rafters.addMaterial(rafter);

        /* Return <partslistmodel>*/
        return rafters;
    }

    /**
     * calculates fascia ("stern") for carport roof
     *
     * @param order
     * @return
     * @throws DataException
     */
    protected PartslistModel calculateFascias(OrderModel order) throws DataException
    {
        /* Set up return PartslistModel */
        PartslistModel fascias = new PartslistModel();

        /* Initialize materials needed needed */
        MaterialModel fasciaLengthBottom = DAO.getMaterial(fasciaLengthBottomID, helptext);
        MaterialModel fasciaLengthTop = DAO.getMaterial(fasciaLengthTopID, helptext);
        MaterialModel fasciaWidthBottom = DAO.getMaterial(fasciaWidthBottomID, helptext);
        MaterialModel fasciaWidthTop = DAO.getMaterial(fasciaWidthTopID, helptext);

        /* set up variables */
        int width = order.getWidth();
        int length = order.getLength();

        /* Begin calculation, update quantity */
        fasciaLengthBottom = itemHelper(fasciaLengthBottom, length);
        fasciaLengthTop = itemHelper(fasciaLengthTop, length);
        fasciaWidthBottom = itemHelper(fasciaWidthBottom, width);
        fasciaWidthTop = itemHelper(fasciaWidthTop, width);

        //We have now calculated the quantity needed by dimension, but need to multiply by carport sides.
        /* Update quantities */
        fasciaLengthBottom.setQuantity(fasciaLengthBottom.getQuantity() * 2); //2 lengths
        fasciaLengthTop.setQuantity(fasciaLengthTop.getQuantity() * 2); //2 lengths
        fasciaWidthBottom.setQuantity(fasciaWidthBottom.getQuantity() * 2); //2 widths
        //fasciaWidthTop is not updated, as we only add top fascia to the front, not the back. (due to water draining)

        /* Set helptext */
        fasciaLengthBottom.setHelptext("understernbrædder til siderne");
        fasciaLengthTop.setHelptext("oversternbrædder til siderne");
        fasciaWidthBottom.setHelptext("understernbrædder til for- & bagende");
        fasciaWidthTop.setHelptext("oversternbrædder til forenden");

        /* Add parts together */
        fascias.addMaterial(fasciaLengthBottom);
        fascias.addMaterial(fasciaLengthTop);
        fascias.addMaterial(fasciaWidthBottom);
        fascias.addMaterial(fasciaWidthTop);

        /* Update prices */
        fascias.getBillOfMaterials().forEach((material) ->
        {
            material.setPrice(material.getPrice() * material.getQuantity());
        });

        /* return partslist */
        return fascias;
    }

    /**
     * Helps calculate item quantity for a materialmodel.
     *
     * @param material material in question
     * @param dimension dimension in question (length, width, etc)
     * @return returns the same materialmodel, now with new quantity.
     */
    private MaterialModel itemHelper(MaterialModel material, int dimension)
    {
        int itemQty = material.getQuantity();
        int remainingDimension = dimension;

        /* Calculate quantity */
        while (remainingDimension > 0)
        {
            itemQty++;
            remainingDimension -= material.getLength();
        }

        /* Update quantity */
        material.setQuantity(itemQty);

        /* return item */
        return material;
    }

    /**
     * calculates bargeboard ("vandbrædt") for carport roof
     *
     * @param order
     * @return
     */
    protected PartslistModel calculateBargeboard(OrderModel order) throws DataException
    {
        /* Set up return <partslistmodel>*/
        PartslistModel bargeboards = new PartslistModel();

        /* Get MaterialModel to return */
        MaterialModel boardsLength = DAO.getMaterial(bargeboardLengthID, helptext);
        MaterialModel boardsWidth = DAO.getMaterial(bargeboardWidthID, helptext);
        MaterialModel boardScrews = DAO.getMaterial(bargeboardScrewsID, helptext);

        /* Initialize variables */
        int length = order.getLength();
        int width = order.getWidth();

        /* Calculation begin */
        boardsLength = itemHelper(boardsLength, length);
        boardsWidth = itemHelper(boardsWidth, width);

        //We have now calculated the quantity needed by dimension, but need to multiply by carport sides.
        /* Update quantities */
        boardsLength.setQuantity(boardsLength.getQuantity() * 2); //two lengths
        boardScrews.setQuantity(1); //pack of 200
        //boardsWidth is not updated, as there only are bargeboards for the front.

        /* Update price */
        boardsLength.setPrice(boardsLength.getQuantity() * boardsLength.getPrice());
        boardsWidth.setPrice(boardsWidth.getQuantity() * boardsWidth.getPrice());

        /* Update helptext */
        boardsLength.setHelptext("vandbrædt på stern i sider");
        boardsWidth.setHelptext("vandbrædt på stern i forende");
        boardScrews.setHelptext("Til montering af stern & vandbrædt");

        /* Add to <partslistmodel> */
        bargeboards.addMaterial(boardsLength);
        bargeboards.addMaterial(boardsWidth);
        bargeboards.addMaterial(boardScrews);

        /* Return <partslistmodel>*/
        return bargeboards;
    }

    /**
     * calculate hulbånd for carport roof
     *
     * @param order
     * @return
     */
    protected PartslistModel calculateBand(OrderModel order) throws DataException
    {
        PartslistModel bandParts = new PartslistModel();
        int bandAmount = 1; //used to determine band quantity. we always want one.

        /*get MaterialModel to return */
        MaterialModel band = DAO.getMaterial(bandID, helptext);
        MaterialModel bandScrews = DAO.getMaterial(bandScrewsID, helptext);

        /* Calculation begin */
        int bandLength = band.getLength(); //10000mm (10m)
        int shedLength = order.getShed_length();
        int carportLength = order.getLength();

        //band runs from shed to front
        int bandCoverLength = (carportLength - shedLength); //band does not cover shed

        int bandEffectiveLength = bandCoverLength * 2; //we need to cover diagonally (two lengths)

        bandEffectiveLength -= bandLength; //we start at one, so lets remove bandLength from calculation.
        while (bandEffectiveLength >= 0) //old version: (bandEffectiveLength - bandLength >= 0)
        {
            ++bandAmount;
            bandEffectiveLength -= bandLength;
        }

        /* update quantities */
        band.setQuantity(bandAmount);
        bandScrews.setQuantity(1); // 250 a package
        //screws calculation too (2 pr. spær crossed)

        /* update price */
        band.setPrice(band.getQuantity() * band.getPrice());
        /* Update helptext */
        band.setHelptext("Til vindkryds på spær");
        bandScrews.setHelptext("Til montering af universalbeslag + hulbånd");
        /* add to PartslistModel */
        bandParts.addMaterial(band);
        bandParts.addMaterial(bandScrews);

        /* Return PartslistModel */
        return bandParts;
    }

    /**
     * calculate universalbeslag & screws for carport roof
     *
     * @param rafters partslistmodel of rafters. need quantity to calculate
     * fittings.
     * @return returns a list of materials (to later add to bill of materials)
     */
    protected PartslistModel calculateFittings(PartslistModel rafters) throws DataException
    {
        /* Rafter amount */
        int rafterAmount = rafters.getBillOfMaterials().get(0).getQuantity(); //hackish solution.

        /*PartsListModel to return */
        PartslistModel fittingsAndScrews = new PartslistModel();

        /* Get standards */
        int screwStandard = rafterFittingScrewStandard; //9

        /* Calculation begin */
        int fittingsAmount = rafterAmount;
        //int screwAmount = (fittingsAmount*screwStandard); //9 screws per fitting

        /* Get materials from database */
        MaterialModel fittingRight = DAO.getMaterial(fittingRightID, helptext);
        MaterialModel fittingLeft = DAO.getMaterial(fittingLeftID, helptext);
        MaterialModel fittingScrews = DAO.getMaterial(fittingScrewsID, helptext);
        //amountOfScrews += (fittingsAmount * screwAmount);

        /* update quantities */
        fittingRight.setQuantity(fittingsAmount);
        fittingLeft.setQuantity(fittingsAmount);
        fittingScrews.setQuantity(1); //1 pack is 250
        //setScrewQty 

        /* update price */
        fittingRight.setPrice((fittingRight.getPrice() * fittingRight.getQuantity()));
        fittingLeft.setPrice((fittingLeft.getPrice() * fittingLeft.getQuantity()));
        //setScrewPrice 

        /* Update helptext */
        fittingRight.setHelptext("Til montering	af spær	på rem");
        fittingLeft.setHelptext("Til montering	af spær	på rem");
        fittingScrews.setHelptext("Til montering af universalbeslag + hulbånd");

        /* Add to PartsListModel */
        fittingsAndScrews.addMaterial(fittingRight);
        fittingsAndScrews.addMaterial(fittingLeft);
        fittingsAndScrews.addMaterial(fittingScrews);
        //addScrews 

        /* Return PartsListModel */
        return fittingsAndScrews;
    }

    /**
     * Adds parts that depends on the roof of choice
     *
     * @param order
     * @return returns dependant items (roof tile dependant)
     * @throws data.exceptions.DataException
     */
    protected PartslistModel calculateDependantParts(OrderModel order) throws DataException, AlgorithmException
    {
        PartslistModel dependantParts = new PartslistModel();

        int roofSelection = order.getRoof_tiles_id();
        //the ID selected for roof tiles. 
        //0 = no roof/no choice. || 28, 29 = plastic. 47, 48 = felt ("tagpap").

        switch (roofSelection) //could also be done with multiple if-statements
        {
            //plastic roof
            case 28:
            case 29:
                dependantParts.addPartslist(calculatePlasticRoof(order));

                break;
            //felt roof
            case 47:
            case 48:
                dependantParts.addPartslist(calculateFeltRoof(order));
                break;
            default:
                throw new AlgorithmException("Fejl: Forkert tagmateriale valgt. Du valgte: " + roofSelection);
        }
        return dependantParts;
    }

    /**
     * Calculates materials needed for the flat plastic roof
     *
     * @param order
     * @return
     * @throws DataException
     */
    protected PartslistModel calculatePlasticRoof(OrderModel order) throws DataException
    {
        /* Set up return <partslistmodel>*/
        PartslistModel plasticRoof = new PartslistModel();

        /* Calculate using helper method */
        plasticRoof.addPartslist(calculatePlasticTiles(order));

        /* Return <partslistmodel>*/
        return plasticRoof;
    }

    /**
     * Calculates roof tiles (plastic) for carport roof
     *
     * @param order
     * @return
     * @throws DataException
     */
    protected PartslistModel calculatePlasticTiles(OrderModel order) throws DataException
    {
        /* Set up return <partslistmodel>*/
        PartslistModel tileAndTileAccessories = new PartslistModel();

        /* calculation done with helper method */
        calculatePlasticTileQuantity(order, tileAndTileAccessories);

        /* Return <partslistmodel>*/
        return tileAndTileAccessories;
    }

    /**
     * Helper method to calculate the plastic tile quantity for an order
     *
     * @param order order in question
     * @param tileOptions the partslistmodel to add materials to and later
     * return
     * @return returns tileOptions
     * @throws DataException
     */
    private PartslistModel calculatePlasticTileQuantity(OrderModel order, PartslistModel tileOptions) throws DataException
    {
        /* Get MaterialModel to return */
        MaterialModel tileLarge = DAO.getMaterial(plasticTileLargeID, helptext); //109x6000
        MaterialModel tileSmall = DAO.getMaterial(plasticTileSmallID, helptext); //109x3600
        MaterialModel tileScrews = DAO.getMaterial(plasticTileScrewID, helptext);

        /* Set up variables */
        int remainingLength = order.getLength();
        //take into account that we need a 5cm extension per width for water drainage.
        int remainingWidth = order.getWidth() + (plasticRoofExtensionStandard * 2); //50mm (5cm) for 2 sides of the carport = 100mm (10cm) extra width for the whole carport.

        //take into account that we need a 2cm overlap between two tiles
        int tileLargeLength = (tileLarge.getLength()) - plasticRoofOverlapStandard; //-200mm for overlap
        int tileLargeWidth = (tileLarge.getWidth()) - plasticRoofOverlapStandard; //-200mm for overlap
        int tileSmallLength = (tileSmall.getLength()) - plasticRoofOverlapStandard; //-200mm for overlap
        int tileSmallWidth = (tileSmall.getWidth()) - plasticRoofOverlapStandard; //-200mm for overlap

        /* Calculation begin */
        //The idea here is to calculate a "row" first (length) and then calculate the amount of rows needed for full roof coverage. (length+width)
        int largeQty = 0;
        int smallQty = 0;
        //length
        while (remainingLength >= 0)
        {
            if (remainingLength >= tileLargeLength)
            {
                largeQty++;
                remainingLength -= tileLargeLength;
            }
            else if (remainingLength >= tileSmallLength)
            {
                smallQty++;
                remainingLength -= tileSmallLength;
            }
            else if (remainingLength > 0 && remainingLength < tileSmallLength)
            {
                //edge case: if we are still not at 0, but the remaining length is smaller than the smallest tile
                smallQty++;
                remainingLength -= tileSmallLength;
            }
        }

        //We now have amount of tiles for one length
        //Lets calculate for the width too.
        int totalAmountLarge = (remainingWidth / tileLargeWidth) * largeQty; //Math.ceil not needed, due to the 200mm overlap we always have excessive amount.
        int totalAmountSmall = (remainingWidth / tileSmallWidth) * smallQty;

        /* Update quantities */
        tileLarge.setQuantity(totalAmountLarge);
        tileSmall.setQuantity(totalAmountSmall);
        //need to update screws too (see rules)
        tileScrews.setQuantity(3);

        /* Update price */
        tileLarge.setPrice(tileLarge.getQuantity() * tileLarge.getPrice());
        tileSmall.setPrice(tileSmall.getQuantity() * tileSmall.getPrice());
        tileScrews.setPrice(tileScrews.getQuantity() * tileScrews.getPrice());

        /* Update quantity */
        tileOptions.addMaterial(tileLarge);
        tileOptions.addMaterial(tileSmall);
        tileOptions.addMaterial(tileScrews);

        return tileOptions;
    }

    /**
     * calculates materials needed for the flat felt roof.
     *
     * @param order
     * @return
     */
    protected PartslistModel calculateFeltRoof(OrderModel order)
    {
        throw new UnsupportedOperationException("Ikke understøttet endnu."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Calculates roof tiles (felt) ("tagpap") for carport roof
     *
     * @param order
     * @return
     */
    protected PartslistModel calculateFeltTiles(OrderModel order)
    {
        return null;
    }

}
