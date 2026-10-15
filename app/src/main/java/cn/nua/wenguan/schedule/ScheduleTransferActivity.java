package cn.nua.wenguan.schedule;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import cn.nua.wenguan.schedule.data.*;
import cn.nua.wenguan.schedule.model.ScheduleRule;
import cn.nua.wenguan.schedule.ui.*;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
public final class ScheduleTransferActivity extends AppearanceActivity {
    private static final int EXPORT=5301,IMPORT=5302;
    private static final ExecutorService WORK=Executors.newSingleThreadExecutor();
    private int format;private TextView status;private boolean busy;
    @Override protected void onCreate(Bundle state){super.onCreate(state);if(state!=null)format=state.getInt("format",0);ScrollView scroll=new ResponsiveScrollView(this);LinearLayout body=EditUi.column(this);body.setPadding(EditUi.dp(this,20),EditUi.dp(this,24),EditUi.dp(this,20),EditUi.dp(this,30));scroll.addView(body);setContentView(scroll);body.addView(EditUi.text(this,"导出 / 恢复课表",25,true));status=EditUi.text(this,"所有操作均在本机，不上传课表。导出仅含课程，不含日程、提醒设置、学校账号密码或Cookie。",14,false);body.addView(status);
        String[] names={"导出 Excel 表格（.xls）","导出日历文件（.ics）","导出课表备份（.json）"},notes={"一行一个上课时段，适合查看和打印","可导入系统或其他兼容日历，不含闹钟","保留开学日期、课程、配色与备注，可在本应用恢复"};
        for(int n=0;n<3;n++){final int type=n;body.addView(MineActionCards.create(this,R.drawable.ic_ui_file,names[n],notes[n],"本地导出",R.color.action_green,false,()->chooseExport(type)));}
        body.addView(MineActionCards.create(this,R.drawable.ic_ui_cloud,"恢复 JSON 课表备份","预览确认后替换课程，个人日程不变","本地恢复",R.color.action_purple,false,this::chooseImport));
        Button back=new Button(this);back.setText("返回");back.setOnClickListener(v->finish());body.addView(back);
    }
    private void chooseExport(int type){if(busy)return;List<ScheduleRule> rules=ScheduleRuleStore.readRules(this);if(rules==null||rules.isEmpty()){toast("当前课表为空或无法读取，未导出");return;}format=type;String[] mime={"application/vnd.ms-excel","text/calendar","application/json"},ext={"xls","ics","json"};try{startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(mime[type]).putExtra(Intent.EXTRA_TITLE,"南艺电子课表-"+ScheduleRepository.today()+"."+ext[type]),EXPORT);}catch(RuntimeException unavailable){toast("系统文件选择器不可用");}}
    private void chooseImport(){if(busy)return;try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"),IMPORT);}catch(RuntimeException missing){toast("系统文件选择器不可用");}}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();if(!"content".equals(uri.getScheme())){toast("请选择系统文件提供器中的文件");return;}if(request==EXPORT)export(uri);else if(request==IMPORT)restore(uri);}
    private void export(Uri uri){List<ScheduleRule> rules=ScheduleRuleStore.readRules(this);if(rules==null||rules.isEmpty()){toast("课表无法读取，未写入文件");return;}LocalDate start=ScheduleRuleStore.savedStartDate(this);int type=format;busy=true;status.setText("正在导出，请稍候…");WORK.execute(()->{try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("文件不可写");if(type==0)ScheduleExport.xls(out,rules,start);else out.write((type==1?ScheduleExport.calendar(rules,start):ScheduleExport.json(rules,start).toString(2)).getBytes(StandardCharsets.UTF_8));runOnUiThread(()->{busy=false;if(!isFinishing()){status.setText("已导出，可以在刚才选择的位置查看。");toast("导出成功");}});}catch(Exception failed){runOnUiThread(()->{busy=false;if(!isFinishing()){status.setText("导出失败；目标文件可能未完整写入，请删除不完整文件后重试。原课表未改变。");toast("导出失败，原课表保留");}});}});}
    private void restore(Uri uri){busy=true;status.setText("正在读取备份…");WORK.execute(()->{try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException();ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(bytes.size()+n>4*1024*1024)throw new IOException("备份超过4MB");bytes.write(buffer,0,n);}JSONObject json=new JSONObject(bytes.toString("UTF-8"));List<ScheduleRule> rules=ScheduleExport.parse(json);LocalDate date=LocalDate.parse(json.getString("startDate"));List<String> conflicts=ScheduleConflicts.courses(rules);runOnUiThread(()->{busy=false;if(isFinishing()||isDestroyed())return;new AlertDialog.Builder(this).setTitle("确认恢复课表").setMessage(rules.size()+"个上课时段\n开学时间："+date+"\n仅替换课程，不删除个人日程。"+(conflicts.isEmpty()?"":"\n存在时间冲突：\n"+String.join("\n",conflicts))).setNegativeButton("取消",null).setPositiveButton("确认恢复",(d,w)->{try{ScheduleRuleStore.save(this,rules,"本地JSON备份",date);ScheduleChanges.refresh(this);status.setText("课表已恢复，小组件和提醒已刷新。");toast("恢复成功");}catch(Exception error){toast("保存失败，原课表保留");}}).show();});}catch(Exception failed){runOnUiThread(()->{busy=false;if(!isFinishing()){status.setText("不是有效的南艺电子课表 JSON 备份，或文件无法读取。原课表未改变。");toast("读取失败，未覆盖课表");}});}});}
    @Override protected void onSaveInstanceState(Bundle state){state.putInt("format",format);super.onSaveInstanceState(state);}
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
}
