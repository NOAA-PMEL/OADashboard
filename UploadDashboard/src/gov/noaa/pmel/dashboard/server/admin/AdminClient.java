/**
 * 
 */
package gov.noaa.pmel.dashboard.server.admin;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.login.CredentialException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import gov.noaa.pmel.dashboard.server.Users;
import gov.noaa.pmel.dashboard.server.Users.UserRole;
import gov.noaa.pmel.dashboard.server.db.myb.MybatisConnectionFactory;
import gov.noaa.pmel.dashboard.server.model.User;
import gov.noaa.pmel.dashboard.util.PasswordUtils;
import gov.noaa.pmel.tws.client.impl.TwsClientImpl.NoopException;
import gov.noaa.pmel.tws.util.ApplicationConfiguration;
import gov.noaa.pmel.tws.util.StringUtils;
import gov.noaa.pmel.tws.util.cli.CLClient;
import gov.noaa.pmel.tws.util.cli.CLCommand;
import gov.noaa.pmel.tws.util.cli.CLOption;
import gov.noaa.pmel.tws.util.cli.CLOptionValue;
import gov.noaa.pmel.tws.util.cli.CLOptions;
import gov.noaa.pmel.tws.util.cli.CommandProcessor;

/**
 * @author kamb
 *
 */
public class AdminClient extends CLClient {

    private static Logger logger;
    
    private static CLOption opt_batch = CLOption.builder().name("batch").flag("b").longFlag("batch")
                                            .requiresValue(false)
                                            .description("batch mode: assume yes at prompts").build();
    
    private static CLOption opt_username = CLOption.builder().name("user").flag("u").longFlag("username")
                                            .requiredOption(true)
                                            .description("username for user-oriented options").build();
    private static CLOption opt_password = CLOption.builder().name("password").flag("pw").longFlag("password")
                                            .description("new user's password - must conform to password complexity rules, should be enclosed in single quotes." +
                                                         " Default: generated secure password.")
                                            .build();
    private static CLOption opt_firstName = CLOption.builder().name("firstName").flag("fn").longFlag("firstname")
                                            .requiredOption(true)
                                            .description("User first name.").build();
    private static CLOption opt_middleName = CLOption.builder().name("middleName").flag("mn").longFlag("middlename")
                                            .description("User middle name or initial.").build();
    private static CLOption opt_lastName = CLOption.builder().name("lastName").flag("ln").longFlag("lastname")
                                            .requiredOption(true)
                                            .description("User last name.").build();
    private static CLOption opt_userOrg = CLOption.builder().name("organization").flag("o").longFlag("org")
//                                            .requiredOption(true)
                                            .description("User organization name.").build();
    private static CLOption opt_email = CLOption.builder().name("email").flag("e").longFlag("email")
                                            .requiredOption(true)
                                            .description("User email.").build();
    private static CLOption opt_phone = CLOption.builder().name("phone").flag("t").longFlag("phone")
                                            .description("User phone. Use \"x####\" to specify an extension.").build();
    private static CLOption opt_target = CLOption.builder().name("targetDb").flag("d").longFlag("db")
                                            .defaultValue("localhost")
                                            .description("Target database. Options: localhost, prod(uction), [hostname] (which may or may not be supported.)").build();
    
    private static CLOption opt_noop = CLOption.builder().name("no-op").flag("x").longFlag("noop")
                                            .requiresValue(false)
                                            .description("Do not perform requested operation.  Output options and parameters and exit.").build();
    
    
    @SuppressWarnings("unused") // found by reflection
    private static CLCommand cmd_addUser = CLCommand.builder().name("add_user")
                                                .command("add")
                                                .description("Add a new user to the dashboard.")
                                                .option(opt_username)
                                                .option(opt_password)
                                                .option(opt_firstName)
                                                .option(opt_middleName)
                                                .option(opt_lastName)
                                                .option(opt_userOrg)
                                                .option(opt_email)
                                                .option(opt_phone)
                                                .option(opt_target)
                                                .option(opt_batch)
                                                .option(opt_noop)
                                                .build();
    
    private CLOptions _clOptions;

    /**
     * 
     */
    public AdminClient() {
        // TODO Auto-generated constructor stub
    }

    public boolean confirm(String message) {
        if ( _clOptions.booleanValue(opt_batch, false)) {
            return true;
        }
        String fullMessage = "Confirm: " + message + " [yN]: ";
        System.out.print(fullMessage);
        Console console = System.console();
        if ( console == null ) { // running in IDE
            return getUserResponse(fullMessage, "N").toLowerCase().startsWith("y"); // use code below.
        }
        String answer = System.console().readLine();
        return answer != null && answer.startsWith("y");
    }
    private static String getUserResponse(String msg, String defaultValue) {
        try {
//            String prompt = msg + (defaultValue != null ? " [" + defaultValue + "]" : "") + " : ";
//            System.out.print(prompt);
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String response = reader.readLine();
            if ( StringUtils.emptyOrNull(response)) {
                response = defaultValue;
            }
            return response;
        } catch (IOException iox) {
            throw new RuntimeException("Exception reading user input.", iox);
        }
    }

    public void doAdd() {
        logger.info("Add user");
        try {
            String target = _clOptions.get(opt_target);
            String userid = _clOptions.get(opt_username);
            User existgUser = Users.getUser(userid);
            if ( existgUser != null ) {
                throw new IllegalStateException("User " + userid + " exists!");
            }
            String pw = _clOptions.get(opt_password);
            if ( pw != null ) {
                logger.info("Checking supplied pw: " + pw);
                PasswordUtils.validatePasswordStrength(pw);
            } else {
                pw = PasswordUtils.generateSecurePassword();
                System.out.println("New user " +userid + " temp password:"+pw);
            }
            User newUser = User.builder()
                            .username(userid)
                            .firstName(_clOptions.get(opt_firstName))
                            .middle(_clOptions.get(opt_middleName))
                            .lastName(_clOptions.get(opt_lastName))
                            .organization(_clOptions.get(opt_userOrg))
                            .email(_clOptions.get(opt_email))
                            .telephone(_clOptions.get(opt_phone))
                            .build();
            if ( confirm("Add user " + newUser + " to target db: " + target)) {
                if ( ! _clOptions.booleanValue(opt_noop, false)) {
                    Users.addUser(newUser, pw, UserRole.Groupie);
                    System.out.print("User " + userid + " added");
                    if ( _clOptions.get(opt_password) == null ) {
                        System.out.println(" with generated password: " + pw);
                    } else {
                        System.out.println(".");
                    }
                } else {
                    System.out.println("No-op requested.  User not added.");
                }
            } else {
                logger.info("User not added.");
            }
        } catch (CredentialException cex) {
            System.err.println("Password unacceptable.");
            System.err.println(PasswordUtils.passwordRules());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    @Override
    public void doCommand(CLCommand command, Map<CLOption, CLOptionValue> optionValues, List<String> arguments) {
        try {
//            checkOptions(command);
            
            _clOptions = new CLOptions(command, optionValues, arguments);
            if ( _clOptions.options().containsKey(opt_target)) {
                String dbEnv = _clOptions.optionValue(opt_target);
                System.out.println("Configuring db for environment: " + dbEnv + " from mybatis-config");
                MybatisConnectionFactory.initialize(dbEnv);
            }
            Method processingMethod = getProcessingMethod(command);
            processingMethod.invoke(this);
            
        } catch (InvocationTargetException itex) {
            if ( itex.getCause() == null ||
                 ! ( itex.getCause() instanceof NoopException )) {
                itex.printStackTrace();
            }
        } catch (NoopException nex) {
            System.out.println(nex.getMessage() + " - Exiting.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @param command
     * @return
     * @throws SecurityException 
     * @throws NoSuchMethodException 
     */
    private Method getProcessingMethod(CLCommand command) {
        String methodName = getMethodName(command);
        Method processingMethod = null;
        Class<?> thisClass = this.getClass();
        try {
            processingMethod = thisClass.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException nsx) {
//            Method[] thisMethods = thisClass.getDeclaredMethods();
            Class<?> superClass = thisClass.getSuperclass();
            if ( CommandProcessor.class.isAssignableFrom(superClass)) {
                try {
                    processingMethod = superClass.getDeclaredMethod(methodName);
                } catch (NoSuchMethodException ns2) {
//                    Method[] superMethods = superClass.getDeclaredMethods();
                    throw new IllegalStateException("No processing method \"" + methodName + 
                                                    "\" found for command " + command.command() +
                                                    " in either " + thisClass.getName() + " or " + 
                                                    superClass.getName()); 
                }
            } else {
                throw new IllegalStateException("No processing method \"" + methodName + 
                                                "\" found for command " + command +
                                                " in " + thisClass.getName());
            }
        }
        return processingMethod;
    }

    /**
     * @param command
     * @return
     */
    private static String getMethodName(CLCommand command) {
        String methodName = command.methodName();
        if ( methodName == null ) {
            String commandName = command.command();
            char[] chars = ("do"+commandName).toCharArray();
            chars[2] = String.valueOf(chars[2]).toUpperCase().charAt(0);
            methodName = String.valueOf(chars);
        }
        return methodName;
    }
    
    private static void dumpProperties() {
        Properties sysprops = System.getProperties();
        for ( Object key : sysprops.keySet()) {
            System.out.println(key+" : " + sysprops.get(key));
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
//            args = new String[] { "add", "-u", "test", "-pw", "ch@ngeM3s00n", "-fn", "Testy", "-ln", "Testarosa", "-e", "nobody@noaa.gov", "-d", "newbock", "-b" };
//            System.out.println("Running AdminClient");
            List<String> filteredArgs = preprocessArgs(args); // sets system property and removes -D args
//            if ( filteredArgs.contains(opt_target.flag()) ||
//                 filteredArgs.contains(opt_target.longFlag())) {
//                int idx = filteredArgs.indexOf(opt_target.flag());
//                if ( idx < 0 ) {
//                    idx = filteredArgs.indexOf(opt_target.longFlag());
//                }
//                String value = filteredArgs.get(idx+1);
//                System.setProperty(MybatisConnectionFactory.DB_ENV_PROPERTY, value);
//            }
            ApplicationConfiguration.Initialize("oap");
            logger = LogManager.getLogger(AdminClient.class);
            logger.debug("Running AdminClient");
            runCommand(filteredArgs.toArray(new String[filteredArgs.size()]));
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
    }

}
