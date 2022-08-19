package cf.wangyu1745.sync.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
 * @since 2022-08-16
 */
@TableName("tunnel")
@Builder
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class Tunnel extends Model<Tunnel> {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("from_server")
    private String fromServer;

    @TableField("from_world")
    private String fromWorld;

    @TableField("from_x")
    private Integer fromX;

    @TableField("from_y")
    private Integer fromY;

    @TableField("from_z")
    private Integer fromZ;

    @TableField("to_server")
    private String toServer;

    @TableField("to_world")
    private String toWorld;

    @TableField("to_x")
    private Integer toX;

    @TableField("to_y")
    private Integer toY;

    @TableField("to_z")
    private Integer toZ;

    @TableField("active")
    private Boolean active;

    @Override
    public Serializable pkVal() {
        return this.id;
    }
}

