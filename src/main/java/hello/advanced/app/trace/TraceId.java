package hello.advanced.app.trace;

import java.util.UUID;

//이 두가지를 묶어서 트랜잭션(트레이스) Id라고 칭합니다.
public class TraceId {
    private String id;
    private int level;

    public TraceId() {
        this.id = createId();
        this.level = 0;
    }

    private String createId() {
        return UUID.randomUUID().toString().substring(0,8); //기존의 uuid는 너무 길어서 앞의 prefix정도만 잘라서 사용할 예정.
    }

    private TraceId(String id, int level){
        this.id = id;
        this.level = level;
    }

    public TraceId createNextId(){
        return new TraceId(id,level+1);
    }

    public TraceId createPreviouslyId(){
        return new TraceId(id,level-1);
    }

    public boolean isFirstLevel(){
        return level == 0;
    }


    //==getter==
    public String getId() {
        return id;
    }

    public int getLevel() {
        return level;
    }
}
