package cn.nua.wenguan.schedule.data;
import cn.nua.wenguan.schedule.model.ScheduleRule;
import java.util.*;

/** Replace explicit rule IDs only. Course names never define the edit scope. */
public final class SlotEditPolicy {
    private SlotEditPolicy() { }
    public static List<ScheduleRule> merge(List<ScheduleRule> current,Set<String> originalIds,List<ScheduleRule> edits) {
        Set<String> present=new HashSet<>();
        for(ScheduleRule rule:current)present.add(rule.getId());
        if(!present.containsAll(originalIds))throw new IllegalArgumentException("原时段已改变，请返回重新编辑");
        List<ScheduleRule> result=new ArrayList<>();
        for(ScheduleRule rule:current)if(!originalIds.contains(rule.getId()))result.add(rule);
        Set<String> ids=new HashSet<>();for(ScheduleRule rule:result)ids.add(rule.getId());
        for(ScheduleRule rule:edits){if(!ids.add(rule.getId()))throw new IllegalArgumentException("时段编号重复，未保存");result.add(rule);}
        return result;
    }
}
