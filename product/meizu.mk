# Keyhandler
PRODUCT_PACKAGES += \
    CMActions \
    com.cyanogenmod.keyhandler

# Doze
PRODUCT_PACKAGES += \
    MeizuDoze \
    lights.mt6753

# Rotation fix at charging screen
ADDITIONAL_DEFAULT_PROPERTIES += \
    ro.sf.hwrotation=0 \
