package fun.iiii.mixedlogin.util;

import com.velocitypowered.api.util.UuidUtils;
import fun.iiii.mixedlogin.type.OfflineUUIDType;

import java.util.UUID;

public class ExtraUuidUtils {

    public static OfflineUUIDType getOfflineUUID(UUID holderUUID, String name) {
        UUID offlineUUID = UuidUtils.generateOfflinePlayerUuid(name);
        UUID pclUUID = UUID.fromString(toUUID(getPCL2UUID(name)));
        if (holderUUID.equals(offlineUUID)){
            return OfflineUUIDType.OFFLINE;
        }
        if (holderUUID.equals(pclUUID)){
            return OfflineUUIDType.PCL;
        }
        return OfflineUUIDType.UNKNOWN;
    }

    private static String getPCL2UUID(String name) {
        String fullUuid = PCL2_strFill(Integer.toHexString(name.length()), "0", 16)
                + PCL2_strFill(Long.toHexString(PCL2_getHash(name)), "0", 16);
        return fullUuid.substring(0, 12) + "3"
                + fullUuid.substring(13, 16) + "9"
                + fullUuid.substring(17, 32);
    }

    private static String PCL2_strFill(String str, String code, int length) {
        if (str.length() > length) {
            return str.substring(0, length);
        }
        return code.repeat(length - str.length()) + str;
    }

    private static long PCL2_getHash(String str) {
        long hash = 5381;
        for (int i = 0; i < str.length(); i++) {
            hash = (hash << 5) ^ hash ^ (long) str.charAt(i);
        }
        return hash ^ 0xA98F501BC684032FL;
    }

    private static String toUUID(String no_) {
        String fullUuid = no_.substring(0, 8) + "-"
                + no_.substring(8, 12) + "-"
                + no_.substring(12, 16) + "-"
                + no_.substring(16, 20) + "-"
                + no_.substring(20, 32);
        return fullUuid;
    }

    public static void main(String[] args) {
//            测试用方法
//        00000000-0000-3006-998f-555b0138dc4d
        String name = "ksqeib";
        UUID get = UUID.fromString(toUUID(getPCL2UUID(name)));
        System.out.println(get);
    }

}
