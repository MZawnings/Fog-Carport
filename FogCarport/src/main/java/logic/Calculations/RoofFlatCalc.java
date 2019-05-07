package logic.Calculations;

import data.DataFacade;
import data.DataFacadeImpl;
import data.exceptions.AlgorithmException;
import data.exceptions.LoginException;
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
 * 1.
 *
 * Vandbrædt: 3x (1x front, 1x each side, 0x back)
 *
 * Universalbeslag: 2x per spær. (Right/Left orientation) (9 screws per beslag)
 *
 * Hulbånd: 1x (10m). 2 screws pr. spær it crosses.
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
 * Lægter: To be determined
 *
 *
 */
public class RoofFlatCalc
{

    /* Rules */
    private int plasticRoofExtensionStandard = 50; //5cm extension beyond carport length
    private int plasticRoofOverlapStandard = 200; //20cm overlap between two tiles
    private int plastictileScrewsStandard = 12;
    private int rafterWidthStandard = 500; //1 rafter per 500mm (50cm)
    private int rafterFittingsStandard = 2; //2 fittings per rafter
    private int rafterFittingScrewStandard = 9;
    private int bargeboardScrewStandard = 4;
    private int bandScrewStandard = 2;

    /* database material IDs (height|width|length)*/
    private int plasticTileSmallID = 29; //0x109x3600
    private int plasticTileLargeID = 28; //0x109x6000
    private int plasticTileScrewID = 22; //200 pcs. a pack
    private int bandScrewsID = 21; //250 pcs. a pack
    private int bandID = 23; //0x0x10000
    private int fittingScrewsID = 21; //250 pcs. a pack
    private int fittingLeftID = 16;
    private int fittingRightID = 15;
    private int rafterSmallID = 5; // 45x195x4800
    private int rafterLargeID = 54; //45x195x6000
    private int fasciaWidthBottomID = 55; //25x200x3600
    private int fasciaLengthBottomID = 56; //25x200x5400
    private int fasciaWidthTopID = 57; //25x125x3600
    private int fasciaLengthTopID = 58; //25x125x5400
    private int bargeboardLengthID = 59;
    private int bargeboardWidthID = 60;
    private int bargeboardScrewsID = 20; //200 a pack

    /* Calculations */
    private int amountOfScrews; //total amount of screws needed

    /* Imports */
    private final DataFacade DAO; //data accessor

    public RoofFlatCalc()
    {
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
     * @throws data.exceptions.LoginException
     */
    protected PartslistModel calculateFlatRoofStructure(OrderModel order) throws AlgorithmException, LoginException
    {
        PartslistModel roofMaterials = new PartslistModel(); //items to be returned to master list
        /* calculate always needed (independent) items */
        roofMaterials.addPartslist(calculateMainParts(order));
        /* calculate items based on type of roof tile */
        roofMaterials.addPartslist(calculateDependantParts(order));

        return roofMaterials;
    }

    /**
     *
     * Calculates main parts that are generally always needed for the flat roof
     * part of the construction.
     *
     * @param order order in question
     *
     * @return
     * @throws data.exceptions.LoginException
     */
    protected PartslistModel calculateMainParts(OrderModel order) throws LoginException
    {
        PartslistModel mainMaterials = new PartslistModel();

        /* Calculate amount of materials needed */
        PartslistModel rafters = calculateRafters(order);
        PartslistModel fascias = calculateFascias(order);
        PartslistModel bargeboards = calculateBargeboard(order);
        PartslistModel bands = calculateBand(order);
        PartslistModel fittings = calculateFittings(rafters);

        mainMaterials.addPartslist(rafters);
        mainMaterials.addPartslist(fascias);
        mainMaterials.addPartslist(bargeboards);
        mainMaterials.addPartslist(bands);
        mainMaterials.addPartslist(fittings);

        return mainMaterials;
    }

    /**
     * Adds parts that depends on the roof of choice
     *
     * @param order
     * @return
     * @throws data.exceptions.AlgorithmException
     */
    protected PartslistModel calculateDependantParts(OrderModel order) throws AlgorithmException, LoginException
    {
        PartslistModel dependantParts = new PartslistModel();

        int roofSelection = order.getRoof_tiles_id();
        //the ID selected for roof tiles. 
        //0 = no roof/no choice. || 28,* 29 = plastic. 47, 48 = felt ("tagpap").

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
                throw new AlgorithmException(1, "Error #1: No suitable roof ID selected");
        }
        return dependantParts;
    }

    /**
     * Calculates materials needed for the flat plastic roof
     *
     * @param order
     * @return
     * @throws data.exceptions.LoginException
     */
    protected PartslistModel calculatePlasticRoof(OrderModel order) throws LoginException
    {
        /* Set up return <partslistmodel>*/
        PartslistModel plasticRoof = new PartslistModel();

        plasticRoof.addPartslist(calculatePlasticTiles(order));

        /* Return <partslistmodel>*/
        return plasticRoof;
    }

    /**
     * calculates materials needed for the flat felt roof.
     *
     * @param order
     * @return
     */
    protected PartslistModel calculateFeltRoof(OrderModel order)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * calculates rafter ("spær") for carport roof
     *
     * we only care about width, then the customer customizes the rafter to
     * their liking.
     *
     * @param order order in question
     * @return returns partlistmodel of items needed
     */
    protected PartslistModel calculateRafters(OrderModel order) throws LoginException
    {
        /* Set up return <model>*/
        PartslistModel rafters = new PartslistModel();
        /* Get MaterialModel to return */
        MaterialModel rafter = DAO.getMaterial(rafterLargeID); //45x195x6000

        /* Set up variables */
        int width = order.getWidth();

        /* Calculation begin */
        int rafterAmount = (width / rafterWidthStandard); //one rafter per 500mm
        int rafterRemainder = (width % rafterWidthStandard);
        while (rafterRemainder > 0)
        {
            rafterAmount++;
            rafterRemainder--;
        }

        /* Update quantities */
        rafter.setQuantity(rafterAmount);

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
     */
    protected PartslistModel calculateFascias(OrderModel order) throws LoginException
    {
        /* Set up return PartslistModel */
        PartslistModel fascias = new PartslistModel();

        /* Initialize materials needed needed */
        MaterialModel fasciaLengthBottom = DAO.getMaterial(fasciaLengthBottomID);
        MaterialModel fasciaLengthTop = DAO.getMaterial(fasciaLengthTopID);
        MaterialModel fasciaWidthBottom = DAO.getMaterial(fasciaWidthBottomID);
        MaterialModel fasciaWidthTop = DAO.getMaterial(fasciaWidthTopID);

        /* set up variables */
        int width = order.getWidth();
        int length = order.getLength();

        /* Begin calculation, update quantities */
        fasciaLengthBottom = itemHelper(fasciaLengthBottom, length);
        fasciaLengthTop = itemHelper(fasciaLengthTop, length);
        fasciaWidthBottom = itemHelper(fasciaWidthBottom, width);
        fasciaWidthTop = itemHelper(fasciaWidthTop, width);

        //We have now calculated the quantity needed by dimension, but need to multiply by carport sides.
        /* Update quantity */
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
    protected MaterialModel itemHelper(MaterialModel material, int dimension)
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
    protected PartslistModel calculateBargeboard(OrderModel order) throws LoginException
    {
        /* Set up return <partslistmodel>*/
        PartslistModel bargeboards = new PartslistModel();

        /* Get MaterialModel to return */
        MaterialModel boardLength = DAO.getMaterial(bargeboardLengthID);
        MaterialModel boardWidth = DAO.getMaterial(bargeboardWidthID);
        MaterialModel boardScrews = DAO.getMaterial(bargeboardScrewsID);

        /* Initialize variables */
        int length = order.getLength();
        int width = order.getWidth();

        /* Calculation begin */
        boardLength = itemHelper(boardLength, length);
        boardWidth = itemHelper(boardWidth, width);

        //We have now calculated the quantity needed by dimension, but need to multiply by carport sides.
        /* Update quantities */
        boardLength.setQuantity(boardLength.getQuantity() * 2); //two lengths
        boardScrews.setQuantity(1); //pack of 200
        //boardWidth is not updated, as there only are bargeboards for the front.

        /* Update price */
        boardLength.setPrice(boardLength.getQuantity() * boardLength.getPrice());
        boardWidth.setPrice(boardWidth.getQuantity() * boardWidth.getPrice());

        /* Update helptext */
        boardLength.setHelptext("vandbrædt på stern i sider");
        boardWidth.setHelptext("vandbrædt på stern i forende");
        boardScrews.setHelptext("Til montering af stern & vandbrædt");

        /* Add to <partslistmodel> */
        bargeboards.addMaterial(boardLength);
        bargeboards.addMaterial(boardWidth);
        bargeboards.addMaterial(boardScrews);

        /* Return <partslistmodel>*/
        return bargeboards;
    }

    /**
     * calculate universalbeslag & screws for carport roof
     *
     * @param rafterAmount amount of rafters needed. calculated in earlier
     * method.
     * @return returns a list of materials (to later add to bill of materials)
     */
    protected PartslistModel calculateFittings(PartslistModel rafters) throws LoginException
    {
        /* Rafter amount */
        int rafterAmount = rafters.getBillOfMaterials().get(0).getQuantity(); //hackish solution. update?

        /*PartsListModel to return */
        PartslistModel fittingsAndScrews = new PartslistModel();

        /* Get standards */
        int fittingStandard = rafterFittingsStandard; //2
        int screwStandard = rafterFittingScrewStandard; //9

        /* Calculation begin */
        int fittingsAmount = (rafterAmount * fittingStandard); //2 fittings per rafter
        //int screwAmount = (fittingsAmount*screwStandard); //9 screws per fitting

        /* Get materials from database */
        MaterialModel fittingRight = DAO.getMaterial(fittingRightID);
        MaterialModel fittingLeft = DAO.getMaterial(fittingLeftID);
        MaterialModel fittingScrews = DAO.getMaterial(fittingScrewsID);
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
        //fittingRight.setHelptext(helptext.get(fittingRight));
        //fittingLeft.setHelptext(helptext.get(fittingLeft));
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
     * calculate hulbånd for carport roof
     *
     * @param order
     * @return
     */
    protected PartslistModel calculateBand(OrderModel order) throws LoginException
    {
        PartslistModel bandParts = new PartslistModel();
        int bandAmount = 1; //used to determine band quantity. we always want one.

        /*get MaterialModel to return */
        MaterialModel band = DAO.getMaterial(bandID);
        MaterialModel bandScrews = DAO.getMaterial(bandScrewsID);

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
     * Calculates roof tiles (plastic) for carport roof
     *
     * @param order
     * @return
     * @throws data.exceptions.LoginException
     */
    protected PartslistModel calculatePlasticTiles(OrderModel order) throws LoginException
    {
        /* Set up return <partslistmodel>*/
        PartslistModel tileAndTileAccessories = new PartslistModel();

        /* Get MaterialModel to return */
        MaterialModel tileLarge = DAO.getMaterial(plasticTileLargeID); //109x6000
        MaterialModel tileSmall = DAO.getMaterial(plasticTileSmallID); //109x3600
        MaterialModel tileScrews = DAO.getMaterial(plasticTileScrewID);
        /* Set up variables */
        int remainingLength = order.getLength();
        //take into account that we need a 5cm extension per width for water drainage.
        int remainingWidth = order.getWidth() + (plasticRoofExtensionStandard * 2); //50mm (5cm) for 2 sides of the carport = 100mm (10cm) extra width for the whole carport.
        //take into account that we need a 2cm overlap between two tiles
        int tileLargeLength = (tileLarge.getLength()) - plasticRoofOverlapStandard; //-200mm for overlap
        int tileLargeWidth = (tileLarge.getWidth()) - plasticRoofOverlapStandard; //-200mm for overlap
        int tileSmallLength = (tileSmall.getLength()) - plasticRoofOverlapStandard; //-200mm for overlap
        int tileSmallWidth = (tileSmall.getWidth()) - plasticRoofOverlapStandard; //-200mm for overlap
////        int largeQty = 0;
////        int smallQty = 0;
        /* Calculation begin */
        int largeQty = (remainingLength / tileLargeLength); //amount of large tiles
        double remainderLength = (remainingLength % tileLargeLength); //any leftover space?
        remainderLength /= tileSmallLength; //had to newline, didnt work properly in one-line.
        int smallQty = 0;
        if (remainderLength > 0)
        {
            smallQty = (int) Math.ceil(remainderLength); //amount of small tiles
        }
        //We now have amount of tiles for one length
        int totalAmountLarge = (int) Math.ceil((remainingWidth / tileLargeWidth) * largeQty);
        int totalAmountSmall = (int) Math.ceil((remainingWidth / tileSmallWidth) * smallQty);

////        while (remainingLength > 0 || remainingWidth > 0)
////        {
////            /* Add large tiles */
////            while (remainingLength > tileLarge.getLength())
////            {
////                largeQty++;
////                remainingLength -= tileLargeLength;
////                remainingWidth -= tileLarge.getWidth();
////            }
////            /* Add small tiles */
////            while (remainingLength > tileSmall.getLength())
////            {
////                smallQty++;
////                remainingLength -= tileSmall.getLength();
////                remainingWidth -= tileSmall.getWidth();
////            }
////
////            //last case where dimensions are bigger than 0, but smaller than tile.
////            if (remainingLength > 0
////                    && remainingLength < tileSmall.getLength()
////                    || remainingWidth > 0
////                    && remainingWidth < tileSmall.getWidth())
////            {
////                smallQty++;
////                remainingLength -= tileSmall.getLength();
////                remainingWidth -= tileSmall.getWidth();
////            }
////
////        }

        /* Update quantities */
        tileLarge.setQuantity(totalAmountLarge);
        tileSmall.setQuantity(totalAmountSmall);
        //need to update screws too (see rules)
        tileScrews.setQuantity(3);

        /* Update price */
        tileLarge.setPrice(tileLarge.getQuantity() * tileLarge.getPrice());
        tileSmall.setPrice(tileSmall.getQuantity() * tileSmall.getPrice());
        tileScrews.setPrice(tileScrews.getQuantity() * tileScrews.getPrice());

        /* Update helptext */
        tileLarge.setHelptext("tagplader monteres på spær");
        tileSmall.setHelptext("tagplader monteres på spær");
        tileScrews.setHelptext("Skruer til tagplader");
        /* Add to <partslistmodel> */
        tileAndTileAccessories.addMaterial(tileLarge);
        tileAndTileAccessories.addMaterial(tileSmall);
        tileAndTileAccessories.addMaterial(tileScrews);

        /* Return <partslistmodel>*/
        return tileAndTileAccessories;
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
