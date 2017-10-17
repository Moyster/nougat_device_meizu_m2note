/*
 * Copyright (C) 2014 MediaTek Inc.
 */

/*
 * Copyright (C) 2016 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.telephony;

import android.os.SystemProperties;

import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.FileReader;
import java.io.IOException;

public class MtkEccList extends PhoneNumberUtils {

    static final String LOG_TAG = "MtkEccList";

    private static ArrayList<EccEntry> mCustomizedEccList = null;
    private static HashMap<String, Integer> mHashMapForNetworkEccCategory = null;
    private static boolean sIsCtaSet = true;

    //***** Constructor
    public MtkEccList() {
	sIsCtaSet = "1".equals(SystemProperties.get("ro.mtk_cta_set"));
	mCustomizedEccList = new ArrayList<EccEntry>();
	parseEccList();
	mHashMapForNetworkEccCategory = new HashMap<String, Integer>();
    }

    /** @hide */
    public static class EccEntry {
	public static final String ECC_LIST_PATH = "/system/etc/ecc_list.xml";
	public static final String ECC_ENTRY_TAG = "EccEntry";
	public static final String ECC_ATTR = "Ecc";
	public static final String CATEGORY_ATTR = "Category";
	public static final String CONDITION_ATTR = "Condition";

	public static final String ECC_NO_SIM = "0";
	public static final String ECC_ALWAYS = "1";
	public static final String ECC_FOR_MMI = "2";

	private String mEcc;
	private String mCategory;
	private String mCondition; // ECC_NO_SIM, ECC_ALWAYS, or ECC_FOR_MMI

	public EccEntry() {
	    mEcc = new String("");
	    mCategory = new String("");
	    mCondition = new String("");
	}

	public void setEcc(String strEcc) {
	    mEcc = strEcc;
	}

	public void setCategory(String strCategory) {
	    mCategory = strCategory;
	}

	public void setCondition(String strCondition) {
	    mCondition = strCondition;
	}

	public String getEcc() {
	    return mEcc;
	}

	public String getCategory() {
	    return mCategory;
	}

	public String getCondition() {
	    return mCondition;
	}

	@Override
	public String toString() {
	    return ("\n" + ECC_ATTR + "=" + getEcc() + ", " + CATEGORY_ATTR + "="
		    + getCategory() + ", " + CONDITION_ATTR + "=" + getCondition());
	}
    }

    /**
     * Parse Ecc List From XML File
     *
     * @param none.
     * @return none.
     * @hide
     */
    private static void parseEccList() {
	mCustomizedEccList.clear();

	// Parse GSM ECC list
	try {
	    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
	    XmlPullParser parser = factory.newPullParser();
	    if (parser == null) {
		Rlog.d(LOG_TAG, "XmlPullParserFactory.newPullParser() return null");
		return;
	    }
	    FileReader fileReader = new FileReader(EccEntry.ECC_LIST_PATH);
	    parser.setInput(fileReader);
	    int eventType = parser.getEventType();
	    EccEntry record = null;

	    while (eventType != XmlPullParser.END_DOCUMENT) {
		switch (eventType) {
		    case XmlPullParser.START_TAG:
			if (parser.getName().equals(EccEntry.ECC_ENTRY_TAG)) {
			    record = new EccEntry();
			    int attrNum = parser.getAttributeCount();
			    for (int i = 0; i < attrNum; ++i) {
				String name = parser.getAttributeName(i);
				String value = parser.getAttributeValue(i);
				if (name.equals(EccEntry.ECC_ATTR))
				    record.setEcc(value);
				else if (name.equals(EccEntry.CATEGORY_ATTR))
				    record.setCategory(value);
				else if (name.equals(EccEntry.CONDITION_ATTR))
				    record.setCondition(value);
			    }
			}
			break;
		    case XmlPullParser.END_TAG:
			if (parser.getName().equals(EccEntry.ECC_ENTRY_TAG) && record != null)
			    mCustomizedEccList.add(record);
			break;
		}
		eventType = parser.next();
	    }
	    fileReader.close();

	    if (sIsCtaSet) {
		String [] emergencyCTAList = {"120", "122", "110", "119"};
		for (String emergencyNum : emergencyCTAList) {
		    record = new EccEntry();
		    record.setEcc(emergencyNum);
		    record.setCategory("0");
		    record.setCondition(EccEntry.ECC_FOR_MMI);

		    boolean bFound = false;
		    int nIndex = 0;
		    for (EccEntry eccEntry : mCustomizedEccList) {
			String ecc = eccEntry.getEcc();
			if (ecc.equals(emergencyNum)) {
			    bFound = true;
			    Rlog.d(LOG_TAG, "[parseEccList]"
				    + "CTA ecc match customized ecc list, ecc=" + ecc);
			    break;
			}
			nIndex++;
		    }

		    if (bFound)
			mCustomizedEccList.set(nIndex, record);
		    else
			mCustomizedEccList.add(record);
		}
	    }
	} catch (XmlPullParserException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public static boolean isEmergencyNumberExt(String number) {
	Rlog.d(LOG_TAG, "[isEmergencyNumberExt], number:" + number );
	if (number == null) {
	    return false;
	}
	String numberPlus = null;
	boolean bSIMInserted = false;

	// Strip the separators from the number before comparing it
	// to the list.
	number = extractNetworkPortionAlt(number);

	// 1. Check ECCs updated by network
	mHashMapForNetworkEccCategory.clear();
	String strEccCategoryList = SystemProperties.get("ril.ecc.service.category.list");
	if (!TextUtils.isEmpty(strEccCategoryList)) {
	    for (String strEccCategory : strEccCategoryList.split(";")) {
		if (!strEccCategory.isEmpty()) {
		    String[] strEccCategoryAry = strEccCategory.split(",");
		    if (2 == strEccCategoryAry.length) {
			mHashMapForNetworkEccCategory.put(strEccCategoryAry[0],
				Integer.parseInt(strEccCategoryAry[1]));
		    }
		}
	    }
	}

	for (String emergencyNum : mHashMapForNetworkEccCategory.keySet()) {
	    numberPlus = emergencyNum + "+";
	    if (emergencyNum.equals(number)
		 || numberPlus.equals(number)) {
		Rlog.d(LOG_TAG, "[isEmergencyNumberExt] match network ecc list");
		return true;
	    }
	}

	// 2. Check ECCs stored at SIMs
	// Read from SIM1
	String numbers = SystemProperties.get("ril.ecclist");
	Rlog.d(LOG_TAG, "[isEmergencyNumberExt] ril.ecclist: " + numbers);
	if (!TextUtils.isEmpty(numbers)) {
	    // searches through the comma-separated list for a match,
	    // return true if one is found.
	    for (String emergencyNum : numbers.split(",")) {
		numberPlus = emergencyNum + "+";
		if (emergencyNum.equals(number)
		     || numberPlus.equals(number)) {
		    Rlog.d(LOG_TAG, "[isEmergencyNumberExt] match ril.ecclist");
		    return true;
		}
	    }

	    bSIMInserted = true;
	}

	// Read from SIM2
	numbers = SystemProperties.get("ril.ecclist1");
	Rlog.d(LOG_TAG, "[isEmergencyNumberExt] ril.ecclist1: " + numbers);
	if (!TextUtils.isEmpty(numbers)) {
	    // searches through the comma-separated list for a match,
	    // return true if one is found.
	    for (String emergencyNum : numbers.split(",")) {
		numberPlus = emergencyNum + "+";
		if (emergencyNum.equals(number)
		     || numberPlus.equals(number)) {
		    Rlog.d(LOG_TAG, "[isEmergencyNumberExt] match ril.ecclist1");
		    return true;
		}
	    }

	    bSIMInserted = true;
	}

	// 3. Check ECCs customized by user
	if (bSIMInserted) {
	    if (mCustomizedEccList != null) {
		for (EccEntry eccEntry : mCustomizedEccList) {
		    if (!eccEntry.getCondition().equals(EccEntry.ECC_NO_SIM)) {
			String ecc = eccEntry.getEcc();
			numberPlus = ecc + "+";
			if (ecc.equals(number)
			     || numberPlus.equals(number)) {
			    Rlog.d(LOG_TAG, "[isEmergencyNumberExt] match"
				    + " customized ecc list");
			    return true;
			}
		    }
		}
	    }
	} else {
	    if (mCustomizedEccList != null) {
		for (EccEntry eccEntry : mCustomizedEccList) {
		    String ecc = eccEntry.getEcc();
		    numberPlus = ecc + "+";
		    if (ecc.equals(number)
			 || numberPlus.equals(number)) {
			Rlog.d(LOG_TAG, "[isEmergencyNumberExt] match"
				+ " customized ecc list when no sim");
			return true;
		    }
		}
	    }
	}

	Rlog.d(LOG_TAG, "[isEmergencyNumberExt] no match");
	return false;
    }

    /**
     * Get the service category for the given ECC number.
     * @param number The ECC number.
     * @return The service category for the given number.
     * @hide
     */
    public static int getServiceCategoryFromEcc(String number) {
	String numberPlus = null;

	// 1. Get category from network
	for (String emergencyNum : mHashMapForNetworkEccCategory.keySet()) {
	    numberPlus = emergencyNum + "+";
	    if (emergencyNum.equals(number)
		 || numberPlus.equals(number)) {
		Integer nSC = mHashMapForNetworkEccCategory.get(emergencyNum);
		if (nSC != null) {
		    Rlog.d(LOG_TAG, "[getServiceCategoryFromEcc] match network ecc list, "
			    + "Ecc= " + number + ", Category= " + nSC);
		    return nSC;
		}
	    }
	}

	// 2. Get category from sim
	// ToDo: EF_Ecc will convey service category later

	// 3. Get category from user-customized
	if (mCustomizedEccList != null) {
	    for (EccEntry eccEntry : mCustomizedEccList) {
		String ecc = eccEntry.getEcc();
		numberPlus = ecc + "+";
		if (ecc.equals(number)
		     || numberPlus.equals(number)) {
		    Rlog.d(LOG_TAG, "[getServiceCategoryFromEcc] match customized ecc list, "
			    + "Ecc= " + ecc + ", Category= " + eccEntry.getCategory());
		    return Integer.parseInt(eccEntry.getCategory());
		}
	    }
	}

	Rlog.d(LOG_TAG, "[getServiceCategoryFromEcc] no matched for Ecc =" + number + ", return 0");
	return 0;
    }

    public static void updateEmergencyNumbersProperty() {
	ArrayList<String> sim1List = new ArrayList<String>();
	ArrayList<String> sim2List = new ArrayList<String>();
	ArrayList<String> fixedList = new ArrayList<String>();
	ArrayList<String> fixedListNoSim = new ArrayList<String>();

	if (mCustomizedEccList != null) {
	    for (EccEntry eccEntry : mCustomizedEccList) {
		String ecc = eccEntry.getEcc();
		if (!eccEntry.getCondition().equals(EccEntry.ECC_NO_SIM)) {
		    fixedList.add(ecc);
		} else if (!TextUtils.isEmpty(ecc)) {
		    fixedListNoSim.add(ecc);
		}
	    }
	}

	// Read from SIM1
	String numbers = SystemProperties.get("ril.ecclist");
	for (String emergencyNum : numbers.split(",")) {
	    if (!TextUtils.isEmpty(emergencyNum))
		sim1List.add(emergencyNum);
	}

	// dedupe
	sim1List.removeAll(fixedList);
	sim1List.addAll(fixedList);
	if (TextUtils.isEmpty(numbers)) {
	    sim1List.removeAll(fixedListNoSim);
	    sim1List.addAll(fixedListNoSim);
	}

	SystemProperties.set("ril.ecclist",TextUtils.join(",", sim1List));

	// Read from SIM2
	numbers = SystemProperties.get("ril.ecclist1");
	for (String emergencyNum : numbers.split(",")) {
	    if (!TextUtils.isEmpty(emergencyNum))
		sim2List.add(emergencyNum);
	}

	// dedupe
	sim2List.removeAll(fixedList);
	sim2List.addAll(fixedList);
	if (TextUtils.isEmpty(numbers)) {
	    sim2List.removeAll(fixedListNoSim);
	    sim2List.addAll(fixedListNoSim);
	}

	SystemProperties.set("ril.ecclist1",TextUtils.join(",", sim2List));
    }
}
