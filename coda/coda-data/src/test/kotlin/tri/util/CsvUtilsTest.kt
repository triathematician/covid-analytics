package tri.util

import org.junit.Test
import tri.util.CsvLineSplitter.MATCH_ONE_MULTILINE_CSV_RECORD
import tri.util.CsvLineSplitter.reconstitute
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CsvUtilsTest {

    @Test
    fun testOneMultilineCsv() {
        assertTrue(MATCH_ONE_MULTILINE_CSV_RECORD.matches("a"))
        assertTrue(MATCH_ONE_MULTILINE_CSV_RECORD.matches(",,a"))
        assertTrue(MATCH_ONE_MULTILINE_CSV_RECORD.matches(",,a,,,,"))
        assertTrue(MATCH_ONE_MULTILINE_CSV_RECORD.matches("a,b"))
        assertTrue(MATCH_ONE_MULTILINE_CSV_RECORD.matches("a,b, c"))
        assertTrue(MATCH_ONE_MULTILINE_CSV_RECORD.matches("a,b, c,"))
        assertTrue(MATCH_ONE_MULTILINE_CSV_RECORD.matches("a,b, c,,d"))
        assertTrue(MATCH_ONE_MULTILINE_CSV_RECORD.matches("""a,b, c,,"e""""))

        assertTrue(MATCH_ONE_MULTILINE_CSV_RECORD.matches("""a,"b c d, e f g""""))
        assertTrue(MATCH_ONE_MULTILINE_CSV_RECORD.matches("""a,"b c d,
e f g""""))
        assertTrue(MATCH_ONE_MULTILINE_CSV_RECORD.matches("""a,"b c d ""e"" f g""""))
        assertTrue(MATCH_ONE_MULTILINE_CSV_RECORD.matches(""""[a, b, c, d, e]""""))
        assertTrue(MATCH_ONE_MULTILINE_CSV_RECORD.matches(""""[a, b, c, d, e]",f,g"""))

        assertFalse(MATCH_ONE_MULTILINE_CSV_RECORD.matches("""a,""""))
        assertFalse(MATCH_ONE_MULTILINE_CSV_RECORD.matches("""a,b,c,""""))

        val x = """tt,CA,004e5d9b-7a90-4064-8c52-579a3aaa4181,06001,,1203 J Street,Union City,94587,Alameda County,University Of The Pacific Arthur A. Dugoni School Of Dentistry (Union City),Region 9,2020-07-06,"""
        val x2 = """"[all_adult_hospital_beds, all_adult_hospital_inpatient_bed_occupied, all_adult_hospital_inpatient_beds, deaths_covid, on_hand_supply_remdesivir_vials, previous_day_admission_adult_covid_confirmed_18-19, previous_day_admission_adult_covid_confirmed_20-29, previous_day_admission_adult_covid_confirmed_30-39, previous_day_admission_adult_covid_confirmed_40-49, previous_day_admission_adult_covid_confirmed_50-59, previous_day_admission_adult_covid_confirmed_60-69, previous_day_admission_adult_covid_confirmed_70-79, previous_day_admission_adult_covid_confirmed_80+, previous_day_admission_adult_covid_confirmed_unknown, previous_day_admission_adult_covid_confirmed, previous_day_admission_adult_covid_suspected_18-19, previous_day_admission_adult_covid_suspected_20-29, previous_day_admission_adult_covid_suspected_30-39, previous_day_admission_adult_covid_suspected_40-49, previous_day_admission_adult_covid_suspected_50-59, previous_day_admission_adult_covid_suspected_60-69, previous_day_admission_adult_covid_suspected_70-79, previous_day_admission_adult_covid_suspected_80+, previous_day_admission_adult_covid_suspected_unknown, previous_day_admission_adult_covid_suspected, previous_day_admission_pediatric_covid_confirmed, previous_day_admission_pediatric_covid_suspected, previous_day_covid_ED_visits, previous_day_remdesivir_used, previous_day_total_ED_visits, staffed_adult_icu_bed_occupancy, staffed_icu_adult_patients_confirmed_and_suspected_covid, staffed_icu_adult_patients_confirmed_covid, total_adult_patients_hospitalized_confirmed_and_suspected_covid, total_adult_patients_hospitalized_confirmed_covid, total_pediatric_patients_hospitalized_confirmed_and_suspected_covid, total_pediatric_patients_hospitalized_confirmed_covid, total_staffed_adult_icu_beds, able_to_maintain_eye_protection, able_to_maintain_gloves, able_to_maintain_lab_nasal_pharyngeal_swabs, able_to_maintain_lab_nasal_swabs, able_to_maintain_lab_viral_transport_media, able_to_maintain_n95_masks, able_to_maintain_PAPRs, able_to_maintain_single_use_gowns, able_to_maintain_surgical_masks, able_to_maintain_ventilator_medications, able_to_maintain_ventilator_supplies, able_to_obtain_eye_protection, able_to_obtain_gloves, able_to_obtain_launderable_gowns, able_to_obtain_n95_masks, able_to_obtain_PAPRs, able_to_obtain_single_use_gowns, able_to_obtain_surgical_masks, able_to_obtain_ventilator_medications, able_to_obtain_ventilator_supplies, n95_respirators_days_available, on_hand_supply_of_eye_protection_in_days, on_hand_supply_of_gloves_in_days, on_hand_supply_of_single_use_surgical_gowns_in_days, on_hand_supply_of_surgical_masks_in_days, on_hand_ventilator_supplies_in_days, PPE_supply_management_source, reusable_isolation_gowns_used, reusable_n95_masks_used, reusable_PAPRs_or_elastomerics_used]",false,false"""
        val x3 = """tt,CA,004e5d9b-7a90-4064-8c52-579a3aaa4181,06001,,1203 J Street,Union City,94587,Alameda County,University Of The Pacific Arthur A. Dugoni School Of Dentistry (Union City),Region 9,2020-07-06,[004e5d9b-7a90-4064-8c52-579a3aaa4181],1.0,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,0,0,,,10,,500,,,,,15,,1,,1,,14,0,,,,,0,0,0,,0,,,,0,0,0,0,0,0,0,0,0,0,0,,,,,,,,,,,,,,0.13924050632911392,"[all_adult_hospital_beds, all_adult_hospital_inpatient_bed_occupied, all_adult_hospital_inpatient_beds, deaths_covid, on_hand_supply_remdesivir_vials, previous_day_admission_adult_covid_confirmed_18-19, previous_day_admission_adult_covid_confirmed_20-29, previous_day_admission_adult_covid_confirmed_30-39, previous_day_admission_adult_covid_confirmed_40-49, previous_day_admission_adult_covid_confirmed_50-59, previous_day_admission_adult_covid_confirmed_60-69, previous_day_admission_adult_covid_confirmed_70-79, previous_day_admission_adult_covid_confirmed_80+, previous_day_admission_adult_covid_confirmed_unknown, previous_day_admission_adult_covid_confirmed, previous_day_admission_adult_covid_suspected_18-19, previous_day_admission_adult_covid_suspected_20-29, previous_day_admission_adult_covid_suspected_30-39, previous_day_admission_adult_covid_suspected_40-49, previous_day_admission_adult_covid_suspected_50-59, previous_day_admission_adult_covid_suspected_60-69, previous_day_admission_adult_covid_suspected_70-79, previous_day_admission_adult_covid_suspected_80+, previous_day_admission_adult_covid_suspected_unknown, previous_day_admission_adult_covid_suspected, previous_day_admission_pediatric_covid_confirmed, previous_day_admission_pediatric_covid_suspected, previous_day_covid_ED_visits, previous_day_remdesivir_used, previous_day_total_ED_visits, staffed_adult_icu_bed_occupancy, staffed_icu_adult_patients_confirmed_and_suspected_covid, staffed_icu_adult_patients_confirmed_covid, total_adult_patients_hospitalized_confirmed_and_suspected_covid, total_adult_patients_hospitalized_confirmed_covid, total_pediatric_patients_hospitalized_confirmed_and_suspected_covid, total_pediatric_patients_hospitalized_confirmed_covid, total_staffed_adult_icu_beds, able_to_maintain_eye_protection, able_to_maintain_gloves, able_to_maintain_lab_nasal_pharyngeal_swabs, able_to_maintain_lab_nasal_swabs, able_to_maintain_lab_viral_transport_media, able_to_maintain_n95_masks, able_to_maintain_PAPRs, able_to_maintain_single_use_gowns, able_to_maintain_surgical_masks, able_to_maintain_ventilator_medications, able_to_maintain_ventilator_supplies, able_to_obtain_eye_protection, able_to_obtain_gloves, able_to_obtain_launderable_gowns, able_to_obtain_n95_masks, able_to_obtain_PAPRs, able_to_obtain_single_use_gowns, able_to_obtain_surgical_masks, able_to_obtain_ventilator_medications, able_to_obtain_ventilator_supplies, n95_respirators_days_available, on_hand_supply_of_eye_protection_in_days, on_hand_supply_of_gloves_in_days, on_hand_supply_of_single_use_surgical_gowns_in_days, on_hand_supply_of_surgical_masks_in_days, on_hand_ventilator_supplies_in_days, PPE_supply_management_source, reusable_isolation_gowns_used, reusable_n95_masks_used, reusable_PAPRs_or_elastomerics_used]",false,false,,41860,"San Francisco-Oakland-Berkeley, CA",,false,false,[tt]"""
        assertTrue(MATCH_ONE_MULTILINE_CSV_RECORD.matches(x))
        assertTrue(MATCH_ONE_MULTILINE_CSV_RECORD.matches(x2))
        assertTrue(MATCH_ONE_MULTILINE_CSV_RECORD.matches(x3))

        val x4 = """tt,AK,021310,02188,021310,PO BOX 43,KOTZEBUE,99752,Northwest Arctic,MANIILAQ HEALTH CENTER,Region 10,2020-10-07,[2a650999-39d8-4e10-a3a2-457bf759323c],1.0,31+,31+,,31+,31+,31+,4-6,Yes,Yes,Yes,Yes,Yes,Yes,Yes,Yes,Yes,Yes,Yes,Yes,Yes,No,false,true,true,,true,false,true,true,true,true,true,"[staff_shortage_nurses_expected_within_week, staff_shortage_physician_expected_within_week, staff_shortage_other_licensed_independent_practitioner_expected_within_week, staff_shortage_temporary_expected_with_in_week, staff_shortage_other_critical_healthcare_personnel_expected_within_week]",[MANAGED_BY_FACILITY],"[Hair Coverings"""
        assertFalse(MATCH_ONE_MULTILINE_CSV_RECORD.matches(x4))
    }

    @Test
    fun testReconstitute() {
        val test = """a,b,"c
d
e",f"""
        val list = test.splitToSequence('\n').reconstitute()
        assertEquals(test, list.first())
    }

}