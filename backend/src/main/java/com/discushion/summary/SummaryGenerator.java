package com.discushion.summary;
interface SummaryGenerator {
    record Result(String status,String summary) {
        public Result {
            if(!java.util.Set.of("SUCCEEDED","FAILED","SOURCE_TOO_SHORT").contains(status)
                || ("SUCCEEDED".equals(status)?summary==null || summary.isBlank():summary!=null))
                throw new IllegalArgumentException("Invalid generation result");
        }
        static Result failed(){return new Result("FAILED",null);}
    }
    boolean available();
    Result generate(String title,String content);
}
