package com.example.affImport.templates;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.example.affImport.ImportUtils;
import com.expl.dblib.wsObj.v1_05.jobObj.Address;
import com.expl.dblib.wsObj.v1_05.jobObj.JobObj;

public class SUNTRANSFERS implements Template {

	@Override
	public boolean checkSubject(String msgSbj) {
		String requiredSubject = "Booking SUNTR_";
		if (msgSbj != null && msgSbj != "" && (msgSbj.startsWith(requiredSubject)) || (msgSbj.startsWith("CHANGES to booking")))
			return true;

		return false;
	}

	public enum SuntransfersTags {

		// *** Job Field ***
		Date("Time:"),
		Time("PU Date Time:"),
		FName("Name:"),
		PU_Code(""),
		PU_Addr("FROM:"),
		PU_City(""),
		Airport_Code(""),
		Airport_Name(""),
		Airline_Code(""),
		Airline_Name("Airline:"),
		Flight_No("Flight number:"),
		AP_InOut(""),
		DO_Code(""),
		DO_Addr("Accommodation address:"),
		DO_City(""),
		DO_Tel(""),
		PU_Tel("Mobile:"),
		PU_Cell(""),
		Tel_Contact(""),
		Tel_Other(""),
		Car_Color(""),
		Car_Tp("Car Type:"),
		Car_Tp1("Car Type Desc:"),
		Pass_No("Total passengers:"),
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
		Job_Id("Booking");
		// *******************************

		String fieldEmailDesc = "";

		private SuntransfersTags(String fieldEmailDesc) {
			this.fieldEmailDesc = fieldEmailDesc;
		}

		public String getFieldEmailDesc() {
			return this.fieldEmailDesc;
		}
	}

	public void saveJob(JobObj jobObj) {
		
		ImportUtils.deleteNewLines();

		String refInJobId = ImportUtils.getNextWordFrom(SuntransfersTags.Job_Id.getFieldEmailDesc());
		String pickUpAddr = ImportUtils.getParamBetweenWords(SuntransfersTags.PU_Addr.getFieldEmailDesc(), "Flight arrival date (local):");
		String dropOffAddr = ImportUtils.getParamBetweenWords(SuntransfersTags.DO_Addr.getFieldEmailDesc(), "Accommodation phone:");
		String passangerRow = ImportUtils.getParamBetweenWords(SuntransfersTags.FName.getFieldEmailDesc(), SuntransfersTags.PU_Tel.getFieldEmailDesc());
		String arrName[] = passangerRow.split(" ");
		String nameFirst = arrName.length >= 1 ? arrName[0] : "";
		String nameLast = arrName.length >= 2 ? arrName[1] : "";
		String passangersPhone = ImportUtils.getParamBetweenWords(SuntransfersTags.PU_Tel.getFieldEmailDesc(), " ===");
		String valDate = ImportUtils.getParamTwoLine(SuntransfersTags.Date.getFieldEmailDesc());
		
		String dateArrival = ImportUtils.getNextWordFrom("Flight arrival date (local):") + " " + ImportUtils.getNextWordFrom("Flight arrival time (local):");
		String airline = ImportUtils.getParamBetweenWords(SuntransfersTags.Airline_Name.getFieldEmailDesc(), SuntransfersTags.Flight_No.getFieldEmailDesc()).trim();
		String flight = ImportUtils.getNextWordFrom(SuntransfersTags.Flight_No.getFieldEmailDesc());
		
		
		int numberOfPassenger = 0;
		try {
			numberOfPassenger = Integer.valueOf(ImportUtils.getNextWordFrom(SuntransfersTags.Pass_No.getFieldEmailDesc()));
		} catch (Exception e) {System.err.println(e.getMessage());}
		
		

		Address addrPickUp = ImportUtils.getPickUp(pickUpAddr, this);
		Address adrDrop = ImportUtils.getDropOff(dropOffAddr, this);
		//	25/04/2016 19:55
		ImportUtils.checkSetArrivalDateTime(addrPickUp, new SimpleDateFormat("dd/MM/yyyy HH:mm"), dateArrival);

		if (airline != null && !airline.trim().equals("") && addrPickUp != null)
			addrPickUp.setAirline(ImportUtils.checkAirline(airline));

		if (airline != null && !flight.trim().equals("") && addrPickUp != null)
			addrPickUp.setFlight(flight);
		
		try {
			valDate = valDate.substring(valDate.indexOf(" "));
		} catch (Exception e) {}
		String tempGetPrice = ImportUtils.getParamSameOrNextOneLine(SuntransfersTags.Fare.getFieldEmailDesc());
		String[] arrPrice = tempGetPrice.split("\\s+");
		double needPrice = 0;
		if (arrPrice.length > 0) {
			try {
				needPrice = Double.valueOf(arrPrice[0]);
			} catch (Exception e) {
				System.err.println(e.getMessage() + " = needPrice = Double.valueOf(arrPrice[0]);");
				needPrice = 0;
			}
		}

		Date date = null;
		try {
			date = new SimpleDateFormat("dd/MM/yyyy HH:mm").parse(valDate);
		} catch (ParseException e) {}

		jobObj.setPickUp(addrPickUp);
		jobObj.setDropOff(adrDrop);
		jobObj.setNameFirst(nameFirst);
		jobObj.setNameLast(nameLast);
		jobObj.setPassengers(numberOfPassenger);
		jobObj.setNumber(ImportUtils.validatePhone(passangersPhone, this));
		jobObj.setDateTime(ImportUtils.getDate(date, this));
		jobObj.setCustId(0);
		jobObj.setRefInJobId(refInJobId);
		jobObj.setCarPrice(ImportUtils.getCarPrice(jobObj, needPrice));
	}
}