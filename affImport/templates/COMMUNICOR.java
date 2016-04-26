package com.example.affImport.templates;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.example.affImport.ImportUtils;
import com.expl.dblib.wsObj.v1_05.jobObj.Address;
import com.expl.dblib.wsObj.v1_05.jobObj.JobObj;

public class COMMUNICOR implements Template { //need for work PDF

    @Override
    public boolean checkSubject(String msgSbj) {
        String requiredSubject = "Communicor Bookings";
        if (msgSbj != null && msgSbj != "" && msgSbj.startsWith(requiredSubject))
            return true;

        return false;
    }

    public enum CabforceTags {

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
        Job_Id("Res#:");
        // *******************************

        String fieldEmailDesc = "";

        private CabforceTags(String fieldEmailDesc) {
            this.fieldEmailDesc = fieldEmailDesc;
        }

        public String getFieldEmailDesc() {
            return this.fieldEmailDesc;
        }
    }

    public void saveJob(JobObj jobObj) throws Exception {
        Calendar cal = Calendar.getInstance();

        String pickUpAddr = ImportUtils.getParamBetweenWords(CabforceTags.PU_Addr.getFieldEmailDesc(), CabforceTags.DO_Addr.getFieldEmailDesc()).trim();
        String dropOffAddr = ImportUtils.getParamBetweenWords(CabforceTags.DO_Addr.getFieldEmailDesc(), CabforceTags.Date.getFieldEmailDesc());
        String passangerRow = ImportUtils.getParamBetweenWords(CabforceTags.FName.getFieldEmailDesc(), CabforceTags.PU_Addr.getFieldEmailDesc());
        String arrName[] = passangerRow.split(" ");
        String nameFirst = arrName.length >= 1 ? arrName[0] : "";
        String nameLast = arrName.length >= 2 ? arrName[1] : "";
        String passangersPhone = ImportUtils.getParamBetweenWords(CabforceTags.PU_Tel.getFieldEmailDesc(), "Affiliate Instructions:");
        String datePickUpString = ImportUtils.getParamBetweenWords(CabforceTags.Date.getFieldEmailDesc(), CabforceTags.Car_Tp1.getFieldEmailDesc()).replaceAll(" at", "");
        String dateArrival = ImportUtils.getParamBetweenWords("Time:", CabforceTags.PU_Tel.getFieldEmailDesc()).trim();
        String airline = ImportUtils.getParamBetweenWords(CabforceTags.Airline_Name.getFieldEmailDesc(), CabforceTags.Flight_No.getFieldEmailDesc()).trim();
        String flight = ImportUtils.getParamBetweenWords(CabforceTags.Flight_No.getFieldEmailDesc(), "Time:").trim();
        int numberOfPassenger = Integer.valueOf(ImportUtils.getNextWordFrom(CabforceTags.Pass_No.getFieldEmailDesc()));

        Address addrPickUp = ImportUtils.getPickUp(pickUpAddr, this);

        if (airline != null && !airline.trim().equals("") && addrPickUp != null)
            addrPickUp.setAirline(airline);

        if (airline != null && !flight.trim().equals("") && addrPickUp != null)
            addrPickUp.setFlight(flight);

        Date datePickUp = null;
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm");
        try {
            datePickUp = formatter.parse(datePickUpString);
            if (dateArrival != null && !dateArrival.equals("")) {
                cal.setTime(datePickUp);
                cal.set(Calendar.HOUR_OF_DAY, Integer.valueOf(dateArrival.split(":")[0]));
                cal.set(Calendar.MINUTE, Integer.valueOf(dateArrival.split(":")[1]));
                addrPickUp.setArrivalDateTime(cal.getTime().getTime());
            }
        } catch (Exception e) {
        }

        String tempGetPrice = ImportUtils.getParamSameOrNextOneLine(CabforceTags.Fare.getFieldEmailDesc());
        double needPrice = 0;
        if (tempGetPrice != null && !tempGetPrice.equals("") && tempGetPrice.split("\\s+").length > 1) {
            String[] arrPrice = tempGetPrice.split("\\s+");
            needPrice = Double.valueOf(arrPrice[0]);
        }

        jobObj.setPickUp(addrPickUp);
        jobObj.setDropOff(ImportUtils.getDropOff(dropOffAddr, this));
        jobObj.setNameFirst(nameFirst);
        jobObj.setNameLast(nameLast);
        jobObj.setNumber(ImportUtils.validatePhone(passangersPhone, this));
        jobObj.setDateTime(ImportUtils.getDate(datePickUp, this));
        jobObj.setCustId(0);
        jobObj.setPassengers(numberOfPassenger);
        jobObj.setCarPrice(ImportUtils.getCarPrice(jobObj, needPrice));
    }
}
