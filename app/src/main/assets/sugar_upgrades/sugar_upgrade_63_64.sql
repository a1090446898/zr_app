-- v64：资产照片记录新增账套编号，迁移/归位照片时优先使用，避免切换账套后照片错归到当前账套
ALTER TABLE ASSET_PHOTO ADD COLUMN ACCTSUITE_ID varchar(50) default '';