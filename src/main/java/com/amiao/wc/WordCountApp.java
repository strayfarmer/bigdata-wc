package com.amiao.wc;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;

public class WordCountApp {

    /**
     * map读取输入文件
     * @author rdb
     *
     */
    public static class MyMapper extends Mapper<LongWritable, Text, Text, FlowBean> {

        @Override
        protected void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException {
            //接收每一行数据
            String line = value.toString();
            //按空格进行分割
            String[] words = line.split("\t");
            FlowBean flowBean = new FlowBean();
            String phone = words[0];
            String up = words[8];
            String down = words[9];
            flowBean.set(Long.valueOf(up), Long.valueOf(down));
            //通过上下文把map处理结果输出
            context.write(new Text(phone), flowBean);
        }
    }

    /**
     * reduce程序，归并统计
     * @author rdb
     *
     */
    public static class MyReduce extends Reducer<Text, FlowBean, Text, FlowBean> {
        @Override
        protected void reduce(Text key, Iterable<FlowBean> values, Context context)
                throws IOException, InterruptedException {
            long up = 0;
            long down = 0;
            for (FlowBean value : values){
                up += value.getUpFlow();
                down += value.getDownFlow();
            }
            //通过上下文把reduce处理结果输出
            context.write(key, new FlowBean(up, down));
        }
    }

    /**
     * 自定义driver:封装mapreduce作业所有信息
     *@param args
     * @throws
     */
    public static void main(String[] args) throws Exception {
        //创建配置
        Configuration configuration = new Configuration();
        //清理已经存在的输出目录
        Path out = new Path(args[1]);
        FileSystem fileSystem = FileSystem.get(configuration);
        if(fileSystem.exists(out)){
            fileSystem.delete(out, true);
            System.out.println("output exists, but it has deleted");
        }
        //创建job
        Job job = Job.getInstance(configuration,"wc");
        //设置job的处理类
        job.setJarByClass(WordCountApp.class);
        //设置作业处理的输入路径
        FileInputFormat.setInputPaths(job, new Path(args[0]));
        //设置map相关的参数
        job.setMapperClass(MyMapper.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(FlowBean.class);
        //设置reduce相关参数
        job.setReducerClass(MyReduce.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(FlowBean.class);
        //设置作业处理的输出路径
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true)? 0 : 1) ;
    }
}
