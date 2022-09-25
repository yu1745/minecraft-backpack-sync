package cf.wangyu1745.sync.mapper;


import cf.wangyu1745.sync.entity.TunnelData;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author wangyu
 * @since 2022-08-16
 */
public interface TunnelDataMapper extends BaseMapper<TunnelData> {
    @Select("delete from tunnel_data where id in (select id from tunnel_data where tunnel_id = #{id} limit 27) returning *")
    List<TunnelData> getTunnelById(long id);
}
