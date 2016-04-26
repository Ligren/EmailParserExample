package com.example.affImport.templates;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.example.affImport.ImportUtils;
import com.expl.dblib.wsObj.v1_05.jobObj.Address;
import com.expl.dblib.wsObj.v1_05.jobObj.JobObj;

public class MEARSGLOBAL implements Template {

    @Override
    public boolean checkSubject(String msgSbj) {
        String requiredSubject = "Mears Global Services Transfer Offer";
        if (msgSbj != null && msgSbj != "" && msgSbj.startsWith(requiredSubject))
            return true;

        return false;
    }

    public enum MearsGlobalTags {

        // *** Job Field ***
        Date("Pickup on:"),
        Time("PU Date Time:"),
        FName("Name:"),
        PU_Code(""),
        PU_Addr("From:"),
        PU_City(""),
        Airport_Code(""),
        Airport_Name(""),
        Airline_Code(""),
        Airline_Name("Airline:"),
        Flight_No("Flt#:"),
        AP_InOut(""),
        DO_Code(""),
        DO_Addr("To:"),
        DO_City(""),
        DO_Tel(""),
        PU_Tel("Contact:"),
        PU_Cell(""),
        Tel_Contact(""),
        Tel_Other(""),
        Car_Color(""),
        Car_Tp("Car Type:"),
        Car_Tp1("Vehicle:"),
        Pass_No("Passenger(s):"),
        Lug_No("No of luggages:"),
        FOP("Payment type Desc:"),
        CC_Type(""),
        CC_No("Card No:"),
        CC_Exp("Exp Date:"),
        CC_AppCode(""),
        CC_AppAmt(""),
        Acct_Id("Account:"),
        Acct_VIP(""),
        Acct_Dept(""),
        Acct_Empl(""),
        Acct_Q1(""),
        Acct_Q1_Code(""),
        Comment("Comment:"),
        Directions(""),
        Fare("Price (prepaid):"),
        Hourly(""),
        Hours(""),
        Hours_Minimum(""),

        // *** Misc Job Field ***
        Msg_User(""),
        Msg_Id(""),
        Msg_Dt(""),
        Msg_Tm(""),
        Msg_Tp(""),
        Msg_CfIP(""),
        Msg_CfPort(""),
        Comp_Id(""),
        Job_Id("Resv#:");
        // *******************************

        String fieldEmailDesc = "";

        private MearsGlobalTags(String fieldEmailDesc) {
            this.fieldEmailDesc = fieldEmailDesc;
        }

        public String getFieldEmailDesc() {
            return this.fieldEmailDesc;
        }
    }

    public void saveJob(JobObj jobObj) throws Exception {

        ImportUtils.deleteNewLines();

        String refInJobId = ImportUtils.getNextWordFrom(MearsGlobalTags.Job_Id.getFieldEmailDesc());
        String pickUpAddr = ImportUtils.getParamBetweenWords(MearsGlobalTags.PU_Addr.getFieldEmailDesc(), MearsGlobalTags.DO_Addr.getFieldEmailDesc()).trim();
        String dropOffAddr = ImportUtils.getParamBetweenWords(MearsGlobalTags.DO_Addr.getFieldEmailDesc(), MearsGlobalTags.Date.getFieldEmailDesc());
        String passangerRow = ImportUtils.getParamBetweenWords(MearsGlobalTags.FName.getFieldEmailDesc(), MearsGlobalTags.PU_Addr.getFieldEmailDesc());
        String arrName[] = passangerRow.split(" ");
        String nameFirst = arrName.length >= 1 ? arrName[0] : "";
        String nameLast = arrName.length >= 2 ? arrName[1] : "";
        String passangersPhone = ImportUtils.getNextWordFrom(MearsGlobalTags.PU_Tel.getFieldEmailDesc());
        String datePickUpString = ImportUtils.getParamBetweenWords(MearsGlobalTags.Date.getFieldEmailDesc(), MearsGlobalTags.Car_Tp1.getFieldEmailDesc()).replaceAll(" at", "");
        String airline = ImportUtils.getParamBetweenWords(MearsGlobalTags.Airline_Name.getFieldEmailDesc(), MearsGlobalTags.Flight_No.getFieldEmailDesc()).trim();
        String flight = ImportUtils.getParamBetweenWords(MearsGlobalTags.Flight_No.getFieldEmailDesc(), "Time:").trim();

        int numberOfPassenger = Integer.valueOf(ImportUtils.getNextWordFrom(MearsGlobalTags.Pass_No.getFieldEmailDesc()));


        Address addrPickUp = ImportUtils.getPickUp(pickUpAddr, this);
        Address addrDroppOff = ImportUtils.getDropOff(dropOffAddr, this);

        if (airline != null && !airline.trim().equals("") && addrPickUp != null) {
            if (addrPickUp != null && addrPickUp.getAirport() != null && !addrPickUp.getAirport().isEmpty())
                addrPickUp.setAirline(airline);
            else
                addrDroppOff.setAirline(airline);
        }

        if (airline != null && !flight.trim().equals("") && addrPickUp != null) {
            if (addrPickUp != null && addrPickUp.getAirport() != null && !addrPickUp.getAirport().isEmpty())
                addrPickUp.setFlight(flight);
            else
                addrDroppOff.setFlight(flight);
        }

        Date datePickUp = null;
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm");
        try {
            datePickUp = formatter.parse(datePickUpString);
        } catch (Exception e) {
            System.err.println("MearsGlobal = datePickUp = formatter.parse(datePickUpString);" + e.getMessage());
        }

        String tempGetPrice = ImportUtils.getParamSameOrNextOneLine(MearsGlobalTags.Fare.getFieldEmailDesc());
        double needPrice = 0;
        if (tempGetPrice != null && !tempGetPrice.equals("") && tempGetPrice.split("\\s+").length > 1) {
            String[] arrPrice = tempGetPrice.split("\\s+");
            needPrice = Double.valueOf(arrPrice[0]);
        }

        jobObj.setPickUp(addrPickUp);
        jobObj.setDropOff(addrDroppOff);
        jobObj.setNameFirst(nameFirst);
        jobObj.setNameLast(nameLast);
        jobObj.setNumber(ImportUtils.validatePhone(passangersPhone, this));
        jobObj.setDateTime(ImportUtils.getDate(datePickUp, this));
        jobObj.setCustId(0);
        jobObj.setPassengers(numberOfPassenger);
        jobObj.setRefInJobId(refInJobId);
        jobObj.setCarPrice(ImportUtils.getCarPrice(jobObj, needPrice));
    }
}
