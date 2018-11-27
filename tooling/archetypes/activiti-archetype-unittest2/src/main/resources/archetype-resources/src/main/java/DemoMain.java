package ${package};

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author YINZHEN
 * @date 2018/11/22 20:00
 * @Description 启动类
 */
@Slf4j
public class DemoMain {

    //processEngineConfiguration引擎配置方式

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoMain.class);

    public static void main(String[] args) throws ParseException {

        log.info("start......");
        //创建流程引擎
        ProcessEngine processEngine = getProcessEngine();

        //部署流程定义文件
        ProcessDefinition processDefinition = getProcessDefinition(processEngine);

        //启动运行流程
        ProcessInstance processInstance = getProcessInstance(processEngine, processDefinition);

        //处理任务
        processTask(processEngine, processInstance);

        log.info("end......");
    }

    private static void processTask(ProcessEngine processEngine, ProcessInstance processInstance) throws ParseException {
        //处理流程任务
        Scanner scanner = new Scanner(System.in);
        while (processInstance != null && !processInstance.isEnded()) {
            TaskService taskService = processEngine.getTaskService();//获取task
            List<Task> list = taskService.createTaskQuery().list();//列举有哪些task需要我们处理
            log.info("待处理任务数量 {} ...", list.size());
            for (Task task : list) {
                log.info("待处理任务 {} ...", task.getName());

                //获取变量的过程
                Map<String, Object> variables = getMap(processEngine, scanner, task);


                //提交工作
                taskService.complete(task.getId(), variables);
                //获取流程最新的实例状态
                processInstance = processEngine.getRuntimeService().createProcessInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .singleResult();

            }
        }
        //关闭
        scanner.close();
    }

    private static Map<String, Object> getMap(ProcessEngine processEngine, Scanner scanner, Task task) throws ParseException {
        //获取form表单输入
        FormService formService = processEngine.getFormService();
        TaskFormData taskFormData = formService.getTaskFormData(task.getId());
        List<FormProperty> formProperties = taskFormData.getFormProperties();

        //将输入的内容进行存储起来
        Map<String, Object> variables = Maps.newHashMap();

        //每次填补数据
        for (FormProperty property : formProperties) {
            String line = null;
            if (StringFormType.class.isInstance(property.getType())) { //输入类型判断，对输入进行判断
                log.info("请输入 {} ? ", property.getName());
                line = scanner.nextLine();
                variables.put(property.getId(), line);
            } else if (DateFormType.class.isInstance(property.getType())) { //如果是日期类型， 要做格式化判断
                log.info("请输入 {} ? 格式(yyyy-MM-dd)", property.getName());
                line = scanner.nextLine();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date date = dateFormat.parse(line);
                variables.put(property.getId(), date);
            } else {
                log.info("类型暂不支持{}", property.getType());
            }
            log.info("您输入的内容是...{} ", line);
        }
        return variables;
    }

    /**
     * 启动运行流程
     *
     * @param processEngine
     * @param processDefinition
     */
    private static ProcessInstance getProcessInstance(ProcessEngine processEngine, ProcessDefinition processDefinition) {
        RuntimeService runtimeService = processEngine.getRuntimeService();//获取运行时对象
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());//根据id去启动
        log.info("启动流程...{}", processInstance.getProcessDefinitionKey());
        return processInstance;
    }

    /**
     * 部署流程定义文件
     *
     * @param processEngine
     * @return
     */
    private static ProcessDefinition getProcessDefinition(ProcessEngine processEngine) {
        RepositoryService repositoryService = processEngine.getRepositoryService(); //对流程定义库进行操作
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
        deploymentBuilder.addClasspathResource("second_approve.bpmn20.xml");
        Deployment deployment = deploymentBuilder.deploy();
        String deploymentId = deployment.getId();
        //根据Id获取流程定义对象
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploymentId)
                .singleResult();
        log.info("流程定义文件:...{}, 流程Id:...{}", processDefinition.getName(), processDefinition.getId());
        return processDefinition;
    }

    /**
     * 创建流程引擎
     *
     * @return
     */
    private static ProcessEngine getProcessEngine() {
        // 通过静态方法 创建了一个基于内存的流程引擎对象
        ProcessEngineConfiguration cfg = ProcessEngineConfiguration
                .createStandaloneInMemProcessEngineConfiguration();
        ProcessEngine processEngine = cfg.buildProcessEngine();

        //获取流程引擎的监听器
        String name = processEngine.getName();
        String version = processEngine.VERSION;

        log.info("流程引擎的名称{}，版本{}", name, version);
        return processEngine;
    }
}
