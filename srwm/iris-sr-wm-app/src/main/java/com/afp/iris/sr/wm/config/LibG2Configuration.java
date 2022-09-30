package com.afp.iris.sr.wm.config;

import com.afp.iptc.g2.config.Config;
import com.afp.iptc.g2.config.ConfigMBean;
import org.springframework.context.annotation.Configuration;

import javax.management.ObjectName;

@Configuration
public class LibG2Configuration {

    public LibG2Configuration() {
        ConfigMBean config = new Config();
        try {
            final ObjectName configMBean = new ObjectName(ConfigMBean.MBEAN_NAME);
            if(!Config.getMBeanServer().isRegistered(configMBean)){
                Config.getMBeanServer().registerMBean(config, configMBean);
            }

        } catch (Exception e) {
            throw new RuntimeException("Unable to register libg2 mbean", e);
        }
    }
}
