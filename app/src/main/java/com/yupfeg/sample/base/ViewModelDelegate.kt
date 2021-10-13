package com.yupfeg.sample.base

import androidx.activity.ComponentActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * ViewModel的委托类
 *
 * * ViewModel在外部调用时，通常是val修饰的不可重复赋值对象，所以这里只重载了getValue()方法
 * @author yuPFeG
 * @date 2020/02/15
 */
class ViewModelDelegate<out T : ViewModel>(private val clazz: KClass<T>,
                                           /**只对Fragment生效，作用域为fragment所在的Activity*/
                                           private val fromActivity : Boolean = false,
                                           /**只对Fragment生效，作用域为Fragment嵌套在的ParentFragment*/
                                           private val fromParentFragment: Boolean = false) {
    /**
     * [ComponentActivity]属性委托方法
     * @param thisRef 进行委托的类的对象
     * @param property 进行委托的属性的对象
     * */
    operator fun getValue(thisRef: ComponentActivity, property: KProperty<*>)
            = obtainViewModelInstance(thisRef)

    /**
     * [Fragment]属性委托方法
     * @param thisRef 进行委托的类的对象
     * @param property 进行委托的属性的对象
     * */
    operator fun getValue(thisRef: Fragment, property: KProperty<*>) =
        when {
            //作用域跟随嵌套的parentFragment
            fromParentFragment -> {
                obtainViewModelInstance(thisRef.requireParentFragment())
            }
            //作用域跟随Fragment所在的Activity
            fromActivity -> {
                obtainViewModelInstance(thisRef.activity as? ComponentActivity
                    ?: throw IllegalStateException("Activity must be extends CompatActivity")
                )
            }
            //作用域为Fragment自身
            else -> obtainViewModelInstance(thisRef)
        }

    /**
     * [DialogFragment]属性委托方法
     * @param thisRef 进行委托的类的对象
     * @param property 进行委托的属性的对象
     * */
    operator fun getValue(thisRef: DialogFragment, property: KProperty<*>) =
        if (fromActivity) {
            //作用域跟随DialogFragment所在的Activity
            obtainViewModelInstance(thisRef.activity as? ComponentActivity
                ?: throw IllegalStateException("Activity must be as AppCompatActivity")
            )
        }else {
            //作用域为DialogFragment自身
            obtainViewModelInstance(thisRef)
        }


    private fun obtainViewModelInstance(lifecycleOwner : LifecycleOwner) : T{
        //每次都从获取指定作用域范围的ViewModel实例（相当于单例）
        val viewModel = when(lifecycleOwner){
            is ComponentActivity -> ViewModelProvider(lifecycleOwner).get(clazz.java)
            is Fragment -> ViewModelProvider(lifecycleOwner).get(clazz.java)
            else -> {
                throw IllegalStateException("Activity or Fragment is null! cant get ViewModel")
            }
        }
        return viewModel
    }

}



