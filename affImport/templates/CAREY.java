package com.example.affImport.templates;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

import com.example.affImport.ImportUtils;
import com.expl.dblib.wsObj.v1_05.jobObj.Address;
import com.expl.dblib.wsObj.v1_05.jobObj.JobObj;

public class CAREY implements Template {

	String[] tmpltsSubj = { "Carey New York", "Embarque New York" };

	@Override
	public boolean checkSubject(String msgSbj) {

		for (String tmpl : tmpltsSubj)
			if (msgSbj != null && msgSbj != "" && msgSbj.startsWith(tmpl))
				return true;

		return false;
	}

	public enum CareyTags {

		// *** Job Field ***
		Date("PU Date:"),
		Time("PU Time:"),
		FName("Passenger Name:"),
		PU_Code(""),
		PU_Addr("Location:"),
		PU_City(""),
		Airport_Code(""),
		Airport_Name(""),
		Airline_Code(""),
		Airline_Name("Airline:"),
		Flight_No("Flt#:"),
		AP_InOut(""),
		DO_Code(""),
		DO_Addr("Location:"),
		DO_City(""),
		DO_Tel(""),
		PU_Tel("Passenger Mobile:"),
		PU_Cell(""),
		Tel_Contact(""),
		Tel_Other(""),
		Car_Color(""),
		Car_Tp("Car Type:"),
		Car_Tp1("Vehicle:"),
		Pass_No("Passengers:"),
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
		Job_Id("Details for");
		// *******************************

		String fieldEmailDesc = "";

		private CareyTags(String fieldEmailDesc) {
			this.fieldEmailDesc = fieldEmailDesc;
		}

		public String getFieldEmailDesc() {
			return this.fieldEmailDesc;
		}
	}

	public void saveJob(JobObj jobObj) throws Exception {

		ImportUtils.deleteNewLines();

		String refInJobId = ImportUtils.getNextWordFrom(CareyTags.Job_Id.getFieldEmailDesc());
		String pickUpAddrLocation = ImportUtils.getParamBetweenWords(CareyTags.DO_Addr.getFieldEmailDesc(), "Address:").replaceAll(" +", " ").replaceAll(	"A/P", "Airport").replaceAll("DO", " ");
		String pickUpAddrAddress = ImportUtils.getParamBetweenWords("Address:", "Carrier:").replaceAll(" +", " ").replaceAll("DO", " ");
		String pickUpAddr = pickUpAddrAddress.contains(pickUpAddrLocation) ? pickUpAddrAddress : pickUpAddrLocation + " " + pickUpAddrAddress;
		String dropOffAddrLocation = ImportUtils.getPrmBtwnWrdsScndMtch(CareyTags.DO_Addr.getFieldEmailDesc(), "Address:").replaceAll(" +", " ").replaceAll("A/P", "Airport").replaceAll("DO", " ");		;
		String dropOffAddrAddress = ImportUtils.getPrmBtwnWrdsScndMtch("Address:", "Carrier:").replaceAll(" +", " ").replaceAll("DO", " ");
		String dropOffAddr = dropOffAddrAddress.contains(dropOffAddrLocation) ? dropOffAddrAddress : dropOffAddrLocation + " " + dropOffAddrAddress;
		String passangerRow = ImportUtils.getParamBetweenWords(CareyTags.FName.getFieldEmailDesc(), CareyTags.PU_Tel.getFieldEmailDesc());
		String arrName[] = passangerRow.split(",");
		String nameFirst = arrName.length >= 1 ? arrName[0].trim() : "";
		String nameLast = arrName.length >= 2 ? arrName[1].trim() : "";
		String passangersPhone = ImportUtils.getParamBetweenWords(CareyTags.PU_Tel.getFieldEmailDesc(), "Pick-Up Information:");
		String datePickUpString = ImportUtils.getParamBetweenWords(CareyTags.Date.getFieldEmailDesc(), CareyTags.Pass_No.getFieldEmailDesc())
				.replaceAll(CareyTags.Time.getFieldEmailDesc(), " ").replaceAll(",", " ").replaceAll(" +", " ");
		String dateArrival = "";
		String airline = "";
		String flight = "";

		if (strCntnsAnthrStr(pickUpAddr, "New York") > 1)
			pickUpAddr = pickUpAddr.replaceFirst("New York", "New York");

		if (strCntnsAnthrStr(dropOffAddr, "New York") > 1)
			dropOffAddr = dropOffAddr.replaceFirst("New York", " ");

		int numberOfPassenger = 0;
		try {
			numberOfPassenger = Integer.valueOf(ImportUtils.getNextWordFrom(CareyTags.Pass_No.getFieldEmailDesc()));
		} catch (Exception e) {
			e.printStackTrace();
		}

		Address addrPickUp = ImportUtils.getPickUp(pickUpAddr, this);

		if (airline != null && !airline.trim().equals("") && addrPickUp != null)
			addrPickUp.setAirline(airline);

		if (airline != null && !flight.trim().equals("") && addrPickUp != null)
			addrPickUp.setFlight(flight);

		Date datePickUp = null;
		// Friday 15Apr2016 11:45
		SimpleDateFormat formatter = new SimpleDateFormat("E ddMMMyyyy HH:mm");
		try {
			datePickUp = formatter.parse(datePickUpString);
			if (dateArrival != null && !dateArrival.equals(""))
				addrPickUp.setArrivalDateTime(datePickUp.getTime());
		} catch (Exception e) {}

		String tempGetPrice = ImportUtils.getParamSameOrNextOneLine(CareyTags.Fare.getFieldEmailDesc());
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
		jobObj.setRefInJobId(refInJobId);
		jobObj.setCarPrice(ImportUtils.getCarPrice(jobObj, needPrice));
	}

	private int strCntnsAnthrStr(String longStr, String findStr) {
		return longStr.length() - longStr.replaceAll(Pattern.quote(findStr.substring(0, 1)) + "(?=" + Pattern.quote(findStr.substring(1)) + ")", "").length();
	}
}