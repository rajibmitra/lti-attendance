UPDATE LTI_KEY
SET APPLICATION_NAME='KstateAttendance'
WHERE APPLICATION_NAME='AviationReporting';

UPDATE CONFIG_ITEM
SET LTI_APPLICATION='KstateAttendance'
WHERE LTI_APPLICATION='AviationReporting';