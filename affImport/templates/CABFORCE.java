package com.example.affImport.templates;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.example.affImport.ImportUtils;
import com.expl.dblib.wsObj.v1_05.jobObj.JobObj;

public class CABFORCE implements Template {

	@Override
	public boolean checkSubject(String msgSbj) {
		String requiredSubject = "INFO: New booking";
		if (msgSbj != null && msgSbj != "" && msgSbj.startsWith(requiredSubject))
			return true;

		return false;
	}

	public enum CabforceTags {

		// *** Job Field ***
		Date("Time:"),
		Time("PU Date Time:"),
		FName("Passenger:"),
		PU_Code(""),
		PU_Addr("Pickup address:"),
		PU_City(""),
		Airport_Code(""),
		Airport_Name(""),
		Airline_Code(""),
		Airline_Name(""),
		Flight_No(""),
		AP_InOut(""),
		DO_Code(""),
		DO_Addr("Destination address:"),
		DO_City(""),
		DO_Tel(""),
		PU_Tel("Contact number:"),
		PU_Cell(""),
		Tel_Contact(""),
		Tel_Other(""),
		Car_Color(""),
		Car_Tp("Car Type:"),
		Car_Tp1("Car Type Desc:"),
		Pass_No("No of Pass:"),
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

		private CabforceTags(String fieldEmailDesc) {
			this.fieldEmailDesc = fieldEmailDesc;
		}

		public String getFieldEmailDesc() {
			return this.fieldEmailDesc;
		}
	}

	public void saveJob(JobObj jobObj) throws Exception {

		String tmpRefInJobId = ImportUtils.getParamBetweenWords(CabforceTags.Job_Id.getFieldEmailDesc(), "INFO: New booking");
		String refInJobId = "";
		try {
			refInJobId = tmpRefInJobId.split("\\/")[1].trim();
		} catch (Exception e) {
			System.err.println("Cabforce, tmpRefInJobId.split('/')[1].trim(); Err = " + e.getMessage());
		}

		String pickUpAddr = ImportUtils.getParamBetweenWords(CabforceTags.PU_Addr.getFieldEmailDesc(), CabforceTags.DO_Addr.getFieldEmailDesc());
		String dropOffAddr = ImportUtils.getParamBetweenWords(CabforceTags.DO_Addr.getFieldEmailDesc(), "Date:");
		String passangerRow = ImportUtils.getParamBetweenWords(CabforceTags.FName.getFieldEmailDesc(), CabforceTags.PU_Tel.getFieldEmailDesc());
		String arrName[] = passangerRow.split(" ");
		String nameFirst = arrName.length >= 1 ? arrName[0] : "";
		String nameLast = arrName.length >= 2 ? arrName[1] : "";
		String passangersPhone = ImportUtils.getNextWordFrom(CabforceTags.PU_Tel.getFieldEmailDesc());
		String datePickUpString = ImportUtils.getParamBetweenWords(CabforceTags.Date.getFieldEmailDesc(), CabforceTags.Fare.getFieldEmailDesc());
		String tempGetPrice = ImportUtils.getNextWordFrom(CabforceTags.Fare.getFieldEmailDesc());

		double needPrice = 0;
		if (tempGetPrice != null && !tempGetPrice.equals(""))
			needPrice = Double.valueOf(tempGetPrice);

//		Wed 20 Apr 2016 18:30
		Date datePickUp = null;
			try {
				datePickUp = new SimpleDateFormat("E dd MMM yyyy HH:mm").parse(datePickUpString);
			} catch (Exception e) {System.err.println("Cabforce = datePickUp = formatter.parse(datePickUpString);" + e.getMessage());}
				

		jobObj.setPickUp(ImportUtils.getPickUp(pickUpAddr, this));
		jobObj.setDropOff(ImportUtils.getDropOff(dropOffAddr, this));
		jobObj.setNameFirst(nameFirst);
		jobObj.setNameLast(nameLast);
		jobObj.setNumber(ImportUtils.validatePhone(passangersPhone, this));
		jobObj.setDateTime(ImportUtils.getDate(datePickUp, this));
		jobObj.setCustId(0);
		jobObj.setRefInJobId(refInJobId);
		jobObj.setCarPrice(ImportUtils.getCarPrice(jobObj, needPrice));
	}
}
