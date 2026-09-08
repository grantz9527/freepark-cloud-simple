package com.freepark.cloud.simple.common.i18n;

/**
 * 统一消息 key，避免业务代码散落硬编码文案。
 */
public final class MessageKeys {

    public static final String COMMON_OK = "common.ok";
    public static final String COMMON_BAD_REQUEST = "common.badRequest";
    public static final String COMMON_SERVER_ERROR = "common.serverError";

    public static final String AUTH_CREDENTIALS_EMPTY = "auth.credentials.empty";
    public static final String AUTH_CREDENTIALS_INVALID = "auth.credentials.invalid";
    public static final String AUTH_ACCOUNT_DISABLED = "auth.account.disabled";
    public static final String AUTH_UNAUTHORIZED = "auth.unauthorized";
    public static final String AUTH_FORBIDDEN = "auth.forbidden";

    public static final String USER_NOT_FOUND = "user.notFound";
    public static final String USER_DUPLICATE = "user.duplicate";
    public static final String USER_USERNAME_INVALID = "user.usernameInvalid";
    public static final String USER_PASSWORD_TOO_SHORT = "user.passwordTooShort";
    public static final String USER_SELF_STATUS = "user.selfStatus";
    public static final String USER_SUPER_ADMIN_PROTECTED = "user.superAdminProtected";

    public static final String COMMON_NOT_FOUND = "common.notFound";

    public static final String PARKING_LOT_CODE_EXISTS = "parking.lot.codeExists";
    public static final String PARKING_LOCATION_NAME_EXISTS = "parking.location.nameExists";
    public static final String PARKING_AREA_NAME_EXISTS = "parking.area.nameExists";
    public static final String PARKING_SPACE_CODE_EXISTS = "parking.space.codeExists";
    public static final String PARKING_LANE_CODE_EXISTS = "parking.lane.codeExists";
    public static final String PARKING_LANE_LOTS_DUPLICATE = "parking.lane.lotsDuplicate";
    public static final String PARKING_BOOTH_NAME_EXISTS = "parking.booth.nameExists";
    public static final String PARKING_BOOTH_CODE_EXISTS = "parking.booth.codeExists";
    public static final String PARKING_ACCESS_JUDGMENT_INVALID_ORDER = "parking.accessJudgment.invalidOrder";
    public static final String PARKING_VALIDATION_FAILED = "parking.validationFailed";
    public static final String PARKING_INTERNAL_VEHICLE_PLATE_EXISTS = "parking.internalVehicle.plateExists";
    public static final String PARKING_WHITELIST_VEHICLE_INVALID_TIME_RANGE = "parking.whitelistVehicle.invalidTimeRange";
    public static final String PARKING_BLACKLIST_VEHICLE_PLATE_EXISTS = "parking.blacklistVehicle.plateExists";
    public static final String PARKING_BLACKLIST_VEHICLE_INVALID_TIME_RANGE = "parking.blacklistVehicle.invalidTimeRange";
    public static final String PARKING_PATTERN_ALLOWLIST_NAME_EXISTS = "parking.patternAllowlist.nameExists";
    public static final String PARKING_PATTERN_ALLOWLIST_PATTERN_EXISTS = "parking.patternAllowlist.patternExists";
    public static final String PARKING_PATTERN_ALLOWLIST_INVALID_PATTERN = "parking.patternAllowlist.invalidPattern";

    public static final String BILLING_DATE_INVALID_TYPE = "billing.date.invalidType";
    public static final String BILLING_DATE_INVALID_RANGE = "billing.date.invalidRange";
    public static final String BILLING_DATE_OVERLAP = "billing.date.overlap";

    public static final String BILLING_DAILY_SLOT_OVERLAP = "billing.daily.slotOverlap";
    public static final String BILLING_DAILY_SLOT_FREE_CONFLICT = "billing.daily.freeConflict";

    public static final String BILLING_SIMULATE_INVALID_RANGE = "billing.simulate.invalidRange";
    public static final String BILLING_SIMULATE_TOO_LONG = "billing.simulate.tooLong";

    public static final String BILLING_PROFILE_NAME_REQUIRED = "billing.profile.nameRequired";
    public static final String BILLING_PROFILE_SEGMENTS_REQUIRED = "billing.profile.segmentsRequired";
    public static final String BILLING_PROFILE_REFERENCED = "billing.profile.referenced";

    public static final String BILLING_RULE_REFERENCED = "billing.rule.referenced";

    public static final String BILLING_BINDING_INVALID_RANGE = "billing.binding.invalidRange";
    public static final String BILLING_BINDING_OVERLAP = "billing.binding.overlap";

    public static final String SETTINGS_INVALID_LOCALE = "settings.invalidLocale";
    public static final String SETTINGS_INVALID_TIMEZONE = "settings.invalidTimezone";
    public static final String SETTINGS_INVALID_PLATE_COLOR = "settings.invalidPlateColor";
    public static final String SETTINGS_EMPTY_PLATE_COLORS = "settings.emptyPlateColors";
    public static final String SETTINGS_DEFAULT_COLOR_NOT_ALLOWED = "settings.defaultColorNotAllowed";
    public static final String SETTINGS_INVALID_CURRENCY = "settings.invalidCurrency";
    public static final String SETTINGS_EMPTY_CURRENCIES = "settings.emptyCurrencies";
    public static final String SETTINGS_DEFAULT_CURRENCY_NOT_ALLOWED = "settings.defaultCurrencyNotAllowed";

    public static final String EDGE_CONFIG_HOST_REQUIRED = "edge.config.hostRequired";
    public static final String EDGE_CONFIG_HOST_INVALID = "edge.config.hostInvalid";
    public static final String EDGE_CONFIG_PORT_INVALID = "edge.config.portInvalid";
    public static final String EDGE_CONFIG_CLIENT_ID_REQUIRED = "edge.config.clientIdRequired";
    public static final String EDGE_CONFIG_CLIENT_ID_INVALID = "edge.config.clientIdInvalid";
    public static final String EDGE_CONFIG_QOS_INVALID = "edge.config.qosInvalid";
    public static final String EDGE_CONFIG_SYNC_INTERVAL_INVALID = "edge.config.syncIntervalInvalid";
    public static final String EDGE_CONFIG_KEEP_ALIVE_INVALID = "edge.config.keepAliveInvalid";
    public static final String EDGE_CONFIG_TOPIC_TOO_LONG = "edge.config.topicTooLong";
    public static final String EDGE_CONFIG_TOPIC_PREFIX_INVALID = "edge.config.topicPrefixInvalid";
    public static final String EDGE_CONFIG_CREDENTIAL_TOO_LONG = "edge.config.credentialTooLong";
    public static final String EDGE_CONFIG_HEARTBEAT_OFFLINE_INVALID = "edge.config.heartbeatOfflineInvalid";
    public static final String EDGE_CONFIG_TEST_FAILED = "edge.config.testFailed";

    public static final String EDGE_NODE_NAME_TOO_LONG = "edge.node.nameTooLong";
    public static final String EDGE_NODE_LOT_NOT_FOUND = "edge.node.lotNotFound";

    private MessageKeys() {
    }
}
