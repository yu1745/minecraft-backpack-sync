package cf.wangyu1745.sync.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 *
 * </p>
 *
 * @author wangyu
 * @since 2022-09-13
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("login")
public class Login extends Model<Login> {

    private static final long serialVersionUID = 1L;
    public static final String OFFLINE = "OFFLINE";

    @TableId("name")
    private String name;

    @TableField("online")
    private String online;

    @TableField("last_login")
    private String lastLogin;

    @TableField("last_data_id")
    private Long lastDataId;

    @Override
    public Serializable pkVal() {
        return this.name;
    }
}
