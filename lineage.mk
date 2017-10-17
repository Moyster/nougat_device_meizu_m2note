# Inherit some common CM stuff.
$(call inherit-product, vendor/cm/config/common_full_phone.mk)

# Inherit device configuration
$(call inherit-product, device/meizu/m2note/device.mk)

# Device identifier. This must come after all inclusions
PRODUCT_DEVICE := m2note
PRODUCT_NAME := lineage_m2note
PRODUCT_BRAND := Meizu
PRODUCT_MODEL := M2 Note
PRODUCT_MANUFACTURER := Meizu
PRODUCT_RELEASE_NAME := M2 Note
PRODUCT_RESTRICT_VENDOR_FILES := false
