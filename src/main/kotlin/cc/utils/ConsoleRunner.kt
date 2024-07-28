package cc.utils

import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.io.FileUrlResource

@Configuration
@EnableConfigurationProperties
@ComponentScan("cc.services")
@ConfigurationPropertiesScan("cc.services")
class ConsoleRunner() {
}

fun consoleRunner(config: String): ApplicationContext {
    val context = object: AnnotationConfigApplicationContext(ConsoleRunner::class.java) {
        override fun getEnvironment(): ConfigurableEnvironment {
            val env = StandardEnvironment()
            val yaml = YamlPropertiesFactoryBean()
            yaml.setResources(FileUrlResource(config))
            env.propertySources.addFirst(PropertiesPropertySource("yaml", yaml.getObject()!!))
            return env
        }
    }

    return context
}

fun <T> ApplicationContext.manualCreate(clazz: Class<T>): T {
    @Suppress("UNCHECKED_CAST")
    return this.autowireCapableBeanFactory.autowire(
        clazz,
        AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false) as T
}