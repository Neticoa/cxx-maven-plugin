#define STR(value) #value
#define STRINGIZE(value) STR(value)
#define SV_FULLVERSION_STR \
  STRINGIZE(SV_MAJORNUMBER) "." \
  STRINGIZE(SV_MINORNUMBER) "." \
  STRINGIZE(SV_BUILDNUMBER) "." \
  STRINGIZE(SV_MODIFICATIONNUMBER)
  
// don't forget empty end line:
 
