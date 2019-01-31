package rpcPackage.rpc.netty_zookeeper_spring.client;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by wangchaohui on 2019/1/30
 */
public class ConsistentHash {

    //key��ʾ��������hashֵ��value��ʾ������
    private static SortedMap<Integer, String> sortedMap = new TreeMap<Integer, String>();

    //�����з���������sortedMap
    public static void initServers( List<String> servers){
        for (int i = 0; i < servers.size(); i++) {
            int hash = getHash(servers.get(i));
            System.out.println("[" + servers.get(i) + "]���뼯����, ��HashֵΪ" + hash);
            sortedMap.put(hash, servers.get(i));
        }
    }

    public  static String getServer(String key) {
        int hash = getHash(key);

        SortedMap<Integer, String> subMap = sortedMap.tailMap(hash);
        if (subMap.isEmpty()) {
            //���û�бȸ�hashֵ��ģ���ӵ�һ��node��ʼ
            Integer i = sortedMap.firstKey();
            //���ض�Ӧ�ķ�����
            return sortedMap.get(i);
        } else {
            //��һ��Key����˳ʱ���ȥ��node������Ǹ����
            Integer i = subMap.firstKey();
            //���ض�Ӧ�ķ�����
            return subMap.get(i);
        }
    }

    //ʹ��FNV1_32_HASH�㷨�����������Hashֵ,���ﲻʹ����дhashCode�ķ���������Ч��û����
    private static int getHash(String str) {
        final int p = 16777619;
        int hash = (int) 2166136261L;
        for (int i = 0; i < str.length(); i++)
            hash = (hash ^ str.charAt(i)) * p;
        hash += hash << 13;
        hash ^= hash >> 7;
        hash += hash << 3;
        hash ^= hash >> 17;
        hash += hash << 5;

        // ����������ֵΪ������ȡ�����ֵ
        if (hash < 0)
            hash = Math.abs(hash);
        return hash;
    }
}
